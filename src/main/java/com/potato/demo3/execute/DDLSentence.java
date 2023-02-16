package com.potato.demo3.execute;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.potato.demo3.entity.ColumnEntity;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.hasor.dbvisitor.dialect.SqlDialect;
import net.hasor.dbvisitor.dialect.provider.H2Dialect;
import net.hasor.dbvisitor.dialect.provider.MySqlDialect;
import net.hasor.dbvisitor.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Splice sql string and execute
 * @author Ekko
 * @date 2022/12/12 15:55
 */
@Slf4j
public class DDLSentence {

    private final boolean      isH2;
    private final JdbcTemplate jdbcTemplate;
    private       SqlDialect   dialect;

    public DDLSentence(boolean isH2, JdbcTemplate jdbcTemplate) {
        this.isH2 = isH2;
        this.jdbcTemplate = jdbcTemplate;
        this.dialect = isH2 ? new H2Dialect() : new MySqlDialect();
    }

    @SneakyThrows
    public void executeCreateTable(String tableName, List<ColumnEntity> columnList) {
        String sql = this.createTable(tableName, columnList);
        try {
            this.jdbcTemplate.execute(sql);
            log.info("Create table, script: " + sql);
        } catch (SQLException e) {
            log.error("Create table failed, msg : " + e.getMessage() + ", script " + sql, e);
        }
    }

    public void executeAlterTable(String tableName, List<ColumnEntity> columnList) {
        List<String> sqlList = this.alterTable(tableName, columnList);
        if (CollectionUtils.isEmpty(sqlList)) {
            return;
        }

        for (String sql : sqlList) {
            try {
                boolean execute = this.jdbcTemplate.execute(sql);
                log.info("运行状态："+execute+" Alter table, script: " + sql);
            } catch (SQLException e) {
                log.error("Alter table failed, msg : " + e.getMessage() + ", script " + sql, e);
            }
        }
    }

    /**
     * 删除多余的列
     */
    @SneakyThrows
    public void deleteRedundantCol(String tableName, List<String> redundantColList) {
        //获取删除列的sql列表
        List<String> sqlList = this.alterTable2(tableName, redundantColList);
        if (sqlList.isEmpty()){
            return;
        }
        sqlList.forEach(sql->{
            try {
                jdbcTemplate.execute(sql);
            } catch (SQLException e) {
                log.error("删除列错误, msg : " + e.getMessage() + ", script " + sql, e);
            }
        });
    }

    private List<String> alterTable2(String tableName, List<String> redundantColList) {
        List<String> sqlList = new ArrayList<>();
        redundantColList.forEach(col->{
            String sb = "alter table " +
                    this.dialect.fmtName(false, tableName) +
                    " drop column " +
                    col;
            sqlList.add(sb);
        });
        return sqlList;
    }

    private List<String> alterTable(String tableName, List<ColumnEntity> columnList) {
        List<String> sqlList = new ArrayList<>();

        for (ColumnEntity column : columnList) {
            StringBuilder sql = new StringBuilder();
            sql.append("alter table ");
            sql.append(this.dialect.fmtName(false, tableName));
            sql.append(" add ");
            sql.append(" " + this.dialect.fmtName(false, column.getColumnName()) + " ");

            String sqlType = column.getSqlType();
            if (isH2) {
                switch (sqlType) {
                    case "tinyint(1)":
                        sql.append("tinyint");
                        sql.append(" ");
                        break;
                    default:
                        sql.append(sqlType);
                        sql.append(" ");
                        break;
                }
            } else {
                sql.append(sqlType);
                sql.append(" ");
            }

            if (column.isNotNull()) {
                sql.append("not null");
                sql.append(" ");
            } else {
                sql.append("null");
                sql.append(" ");
            }

            if (StringUtils.isNotBlank(column.getDefaultValue())) {
                String defaultValue = column.getDefaultValue();
                switch (defaultValue) {
                    case "NULL":
                    case "CURRENT_TIMESTAMP":
                        sql.append("default ");
                        sql.append(defaultValue);
                        sql.append(" ");
                        break;
                    default:
                        sql.append("default ");
                        sql.append("'");
                        sql.append(defaultValue);
                        sql.append("'");
                        sql.append(" ");
                        break;
                }
            }

            if (StringUtils.isNotBlank(column.getComment())) {
                sql.append("comment ");
                sql.append("'");
                sql.append(column.getComment());
                sql.append("'");
            }

            sql.append(";");
            sqlList.add(sql.toString());
        }
        return sqlList;
    }

    private String createTable(String tableName, List<ColumnEntity> columnList) {
        StringBuilder sql = new StringBuilder();
        String primaryKeyColumn = null;

        sql.append("create table if not exists ");
        sql.append(this.dialect.fmtName(false, tableName));

        sql.append("(");
        for (int i = 0; i < columnList.size(); i++) {
            ColumnEntity column = columnList.get(i);
            sql.append(" " + this.dialect.fmtName(false, column.getColumnName()) + " ");
            String sqlType = column.getSqlType();
            if (isH2) {
                switch (sqlType) {
                    case "tinyint(1)":
                        sql.append("tinyint");
                        sql.append(" ");
                        break;
                    default:
                        sql.append(sqlType);
                        sql.append(" ");
                        break;
                }
            } else {
                sql.append(sqlType);
                sql.append(" ");
            }

            if (column.isZeroFill()) {
                sql.append("zerofill");
                sql.append(" ");
            }

            if (column.isUnsigned()) {
                sql.append("unsigned");
                sql.append(" ");
            }

            if (column.isNotNull()) {
                sql.append("not null");
                sql.append(" ");
            } else {
                sql.append("null");
                sql.append(" ");
            }

            if (column.isAutoincrement()) {
                sql.append("auto_increment");
                sql.append(" ");
            }

            if (column.isPrimaryKey()) {
                primaryKeyColumn = column.getColumnName();
            }

            if (StringUtils.isNotBlank(column.getDefaultValue())) {
                String defaultValue = column.getDefaultValue();
                switch (defaultValue) {
                    case "NULL":
                    case "CURRENT_TIMESTAMP":
                        sql.append("default ");
                        sql.append(defaultValue);
                        sql.append(" ");
                        break;
                    default:
                        sql.append("default ");
                        sql.append("'");
                        sql.append(defaultValue);
                        sql.append("'");
                        sql.append(" ");
                        break;
                }
            }

            if (StringUtils.isNotBlank(column.getComment())) {
                sql.append("comment ");
                sql.append("'");
                sql.append(column.getComment());
                sql.append("'");
            }

            if (i != columnList.size() - 1) {
                sql.append(",");
            } else if (StringUtils.isNotBlank(primaryKeyColumn)) {
                sql.append(",");
                sql.append(" ");
                sql.append("PRIMARY KEY");
                sql.append("(");
                sql.append("`");
                sql.append(primaryKeyColumn);
                sql.append("`");
                sql.append(")");
            }

        }

        sql.append(") ");

        if (!isH2) {
            sql.append("engine=innodb ");
            sql.append("default ");
            sql.append("charset=utf8mb4;");
        }

        return sql.toString();
    }

    /**
     * 删除索引
     */
    public void deleteIndexes(Set<String> indexNames,String tableName) {
        if (indexNames==null||indexNames.isEmpty()) return;
        for (String index : indexNames) {
            String s="alter table "+tableName+" drop index "+index;
            try {
                jdbcTemplate.execute(s);
            } catch (SQLException e) {
                e.printStackTrace();
                log.info("删除索引"+index+"失败,"+e);
            }
        }
    }

    /**
     * 添加索引
     */
    public void addIndex(String tableName, String indexName, String[] indexCol, boolean unique) {
        StringBuilder sb;
        sb=new StringBuilder("alter table ").append(tableName);
        if (unique){
            sb.append(" add unique index ");
        }else{
            sb.append(" add index ");
        }
        sb.append(indexName).append("(");
        for (String s : indexCol) {
            sb.append(s)
                    .append(",");
        }
        sb.delete(sb.length()-1,sb.length());
        sb.append(");");
        try {
            jdbcTemplate.execute(sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            log.info("索引添加失败sql:"+ sb+" e:"+e);
        }
    }

    /**
     * 添加真实数据
     */
    @SneakyThrows
    public void insertTruthData(String sql) {
        jdbcTemplate.execute(sql);
    }
}
