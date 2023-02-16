package com.potato.demo3.init;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.potato.demo3.config.ScanPackageConfig;
import com.potato.demo3.entity.ColumnEntity;
import com.potato.demo3.execute.DDLSentence;
import com.potato.demo3.execute.DQLSentence;
import com.potato.demo3.insert.InsertData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.loader.CobbleClassScanner;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dbvisitor.jdbc.ConnectionCallback;
import net.hasor.dbvisitor.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scan all table annotations, generate table and filed
 * @author Ekko
 * @date 2022/12/13 11:15
 */
@Slf4j
public class InitTableAndFiled {

    private final JdbcTemplate jdbcTemplate;

    public InitTableAndFiled(Connection conn) {
        this.jdbcTemplate = new JdbcTemplate(conn);
    }

    private static Set<Class<?>> getAnnotatedClass() {
        ClassPathResourceLoader loader = new ClassPathResourceLoader();
        CobbleClassScanner scanner = new CobbleClassScanner(loader);
        return scanner.getClassSet(new String[] { ScanPackageConfig.SCAN_PACKAGE_ENTITY }, context -> {
            if (context.getClassInfo() != null) {
                for (String anno : context.getClassInfo().annos) {
                    if (anno.equals(TableName.class.getName())) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    public void scanAndGenerate() {
        Set<Class<?>> annotatedClass = getAnnotatedClass();
        boolean isH2 = isH2();
        DQLSentence dqlSentence = new DQLSentence(isH2, this.jdbcTemplate); // use for check
        DDLSentence ddlSentence = new DDLSentence(isH2, this.jdbcTemplate); // gen ddl sql
        int createTableCount=0,addColCount=0;
        for (Class<?> clazz : annotatedClass) {
            TableName tableAnn = clazz.getAnnotation(TableName.class);
            if (tableAnn == null) {
                continue;
            }

            // First check whether class has 'TableName' annotation
            String tableName = tableAnn.value();
            Field[] declaredFields = clazz.getDeclaredFields();

            // has diff columns
            List<ColumnEntity> columnList = generateColumnList(isH2, declaredFields, tableName);
            if (columnList.isEmpty()) {
                continue;
            }

            boolean tableExist = dqlSentence.queryTable(tableName);
            if (!tableExist) {
                // table does not contained in database, use create sentence to build
                ddlSentence.executeCreateTable(tableName, columnList);
                log.info("创建表："+tableName);
                createTableCount++;
            } else if (CollectionUtils.isNotEmpty(columnList)) {
//                List<String> redundantColList=findRedundantCol(declaredFields,tableName);
                // 添加缺少的列
                ddlSentence.executeAlterTable(tableName, columnList);
                log.info("在表"+tableName+"中创建列："+columnList.toString());
                addColCount++;
//                //删除多余的列
//                ddlSentence.deleteRedundantCol(tableName,redundantColList);
            }
        }
        log.info("创建表的数量："+createTableCount);
        log.info("添加列的数量："+addColCount);
        //添加索引
        scanIndex(annotatedClass);
        //插入真实数据
        insertTruthData(dqlSentence,ddlSentence);

        log.info("Init tables and filed finish!");
    }

    //添加真实数据
    private void insertTruthData(DQLSentence dqlSentence, DDLSentence ddlSentence) {
        InsertData[] insertData=InsertData.SQL_LIST;
        int count=0;
        for (InsertData data : insertData) {
            String tableName=data.getTableName();
            int id=data.getId();
            if (!dqlSentence.queryExist(tableName, id)){
                ddlSentence.insertTruthData(data.getSql());
                count++;
            }
        }
        log.info("添加数据数量："+count);
    }


    /**
     * 扫描索引，更新索引
     */
    public void scanIndex(Set<Class<?>> annotatedClass) {
        DQLSentence dqlSentence = new DQLSentence(false, this.jdbcTemplate);
        DDLSentence ddlSentence = new DDLSentence(false, this.jdbcTemplate);
        int addIndexCount=0;
        for (Class<?> clazz : annotatedClass) {
            TableName tableAnn = clazz.getAnnotation(TableName.class);
            if (tableAnn == null) {
                continue;
            }
            String tableName = tableAnn.value();
            //删除索引
            Set<String> indexes = dqlSentence.queryIndexes(tableName);
            ddlSentence.deleteIndexes(indexes,tableName);
            log.info("删除索引:"+indexes.toString());
            //添加索引
            IndexDescribe[] indexDescribe = clazz.getAnnotationsByType(IndexDescribe.class);
            if (indexDescribe.length==0){
                continue;
            }
            for (IndexDescribe describe : indexDescribe) {
                String[] indexCol = describe.value();
                String indexName= describe.name();
                boolean unique= describe.unique();
                ddlSentence.addIndex(tableName,indexName,indexCol,unique);
                addIndexCount++;
                log.info("添加索引："+indexName);
            }

        }

        log.info("添加索引数量："+addIndexCount);

    }

    /**
     * 找到冗余的列
     */
    private List<String> findRedundantCol(Field[] declaredFields, String tableName) {
        DQLSentence dqlSentence=new DQLSentence(false,jdbcTemplate);
        //查询到本地数据库的列
        List<String> localColList =dqlSentence.queryField(tableName);
        //查询当前版本数据库的列
        List<String> currentColList = new ArrayList<>();
        for (Field field : declaredFields) {
            ColumnDescribe annotation = field.getAnnotation(ColumnDescribe.class);
            if (annotation!=null){
                currentColList.add(annotation.value());
            }else{
                currentColList.add(humpToUnderline(field.getName()));
            }
        }
        //取差集
        localColList.removeAll(currentColList);
        return localColList;
    }

    private List<ColumnEntity> generateColumnList(boolean isH2, Field[] colFields, String tableName) {
        List<ColumnEntity> columnList = new ArrayList<>();
        for (Field colField : colFields) {
            ColumnDescribe colInfoAnno = colField.getAnnotation(ColumnDescribe.class);

            TableId autoColAnno = colField.getAnnotation(TableId.class);
            ColumnEntity column = new ColumnEntity();
            if (colInfoAnno != null) {
                //如果列标记为不存在，则不需要添加
                if (!colInfoAnno.exist()){
                    continue;
                }

                // Check whether table contains this filed
                DQLSentence dqlSentence = new DQLSentence(isH2, this.jdbcTemplate);

                //如果字段名为空，说明可以用驼峰转成该名称
                String val=colInfoAnno.value();
                if (StringUtils.isBlank(val)){
                    val=humpToUnderline(colField.getName());
                }

                boolean hasColumn = dqlSentence.queryField(tableName,val );
                if (hasColumn) {
                    continue;
                }

                //如果类型为空，说明可以直接进行判断
                String typeStr=colInfoAnno.type();
                if (typeStr.equals("decimal")){
                    typeStr="decimal(5,2)";
                }

                if (StringUtils.isBlank(typeStr)){
                    typeStr="varchar";
                    Class<?> typeClass=colField.getType();

                    if (typeClass==Long.class||typeClass==long.class){
                        typeStr="bigint";
                    }else if (typeClass==Date.class){
                        typeStr="datetime";
                    }else if (typeClass==String.class){
                        typeStr="varchar";
                    }else if (typeClass==boolean.class||typeClass==Boolean.class){
                        typeStr="tinyint";
                    }else if (typeClass==int.class||typeClass==Integer.class){
                        typeStr="int";
                    }else if (typeClass==Double.class||typeClass==double.class){
                        typeStr="decimal(5,2)";
                    }
                }

                //修改字段长度
                int length=colInfoAnno.length();
                if (length==-1){
                    if (StringUtils.isBlank(typeStr)){
                        Class<?> typeClass=colField.getType();
                        if (typeClass==Long.class||typeClass==long.class){
                            length=20;
                        }else if (typeClass==String.class){
                            length=255;
                        }else if (typeClass==boolean.class||typeClass==Boolean.class){
                            length=1;
                        }else if (typeClass==int.class||typeClass==Integer.class){
                            length=11;
                        }
                    }
                }



                // If not contain, create column bean
                column.setColumnName(val);
                if (length==-1){
                    column.setSqlType(typeStr);
                }else{
                    column.setSqlType(typeStr+"("+length+")");
                }
                column.setDefaultValue(colInfoAnno.defaultValue());
                column.setNotNull(colInfoAnno.isNotNull());
                column.setComment(colInfoAnno.comment());
                column.setZeroFill(colInfoAnno.isZeroFill());
                column.setUnsigned(colInfoAnno.isUnsigned());

                // 'TableId' annotation must be used with 'com.potato.demo3.init.ColumnDescribe' annotation,
                //  it come from MyBatis Plus and identify to primary key
                if (autoColAnno != null) {
                    column.setPrimaryKey(true);
                    IdType type = autoColAnno.type();

                    if (type == IdType.AUTO) {
                        column.setAutoincrement(true);
                    }
                }
            }else{
                //如果没有配置注解，默认使用该字段的下划线名称作为列名
                String colName=humpToUnderline(colField.getName());
                DQLSentence dqlSentence = new DQLSentence(isH2, this.jdbcTemplate);
                boolean hasColumn = dqlSentence.queryField(tableName, colName);
                if (hasColumn) {
                    continue;
                }

                String typeStr="varchar(255)";
                Class<?> typeClass=colField.getType();
                if (typeClass==Long.class||typeClass==long.class){
                    typeStr="bigint";
                }else if (typeClass==Date.class){
                    typeStr="datetime";
                }else if (typeClass==String.class){
                    typeStr="varchar(255)";
                }else if (typeClass==boolean.class||typeClass==Boolean.class){
                    typeStr="tinyint";
                }else if (typeClass==int.class||typeClass==Integer.class){
                    typeStr="int";
                }else if (typeClass==Double.class||typeClass==double.class){
                    typeStr="decimal(5,2)";
                }


                column.setColumnName(colName);
                column.setSqlType(typeStr);
                column.setNotNull(false);
                column.setZeroFill(false);
                column.setUnsigned(false);

                if (column.getColumnName().equals("id")){
                    column.setPrimaryKey(true);
                    IdType type = autoColAnno.type();
                    if (type == IdType.AUTO) {
                        column.setAutoincrement(true);
                    }
                    column.setNotNull(true);
                }


            }
            columnList.add(column);
        }
        return columnList;
    }

    /**
     * 驼峰转下划线 userNameAndAge->user_name_and_age
     */
    public static String humpToUnderline(String str) {
        String regex = "([A-Z])";
        Matcher matcher = Pattern.compile(regex).matcher(str);
        while (matcher.find()) {
            String target = matcher.group();
            str = str.replaceAll(target, "_"+target.toLowerCase());
        }
        return str;
    }

    @SneakyThrows
    private boolean isH2() {
        return this.jdbcTemplate.execute((ConnectionCallback<Boolean>) con -> {
            String jdbcURL = con.getMetaData().getURL();
            return StringUtils.startsWithIgnoreCase(jdbcURL, "jdbc:h2:");
        });
    }


}
