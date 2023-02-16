package com.potato.demo3.execute;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.hasor.dbvisitor.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ekko
 * @date 2022/12/12 15:19
 */
@Slf4j
public class DQLSentence {

    private final String       H2_QUERY_TABLE     = "select table_name from information_schema.tables where table_name = ?";
    private final String       MYSQL_QUERY_TABLE  = "select table_name from information_schema.tables where table_name = ?";
    private final String       H2_QUERY_COLUMN    = "select column_name from information_schema.columns where table_name = ?";
    private final String       MYSQL_QUERY_COLUMN = "select column_name from information_schema.columns where table_name = ?";
    private final boolean      isH2;
    private final JdbcTemplate jdbcTemplate;

    public DQLSentence(boolean isH2, JdbcTemplate jdbcTemplate) {
        this.isH2 = isH2;
        this.jdbcTemplate = jdbcTemplate;
    }

    @SneakyThrows
    public boolean queryTable(String tableName) {
        if (this.isH2) {
            tableName = tableName.toUpperCase();
        } else {
            tableName = tableName.toLowerCase();
        }

        String sql = this.isH2 ? H2_QUERY_TABLE : MYSQL_QUERY_TABLE;
        try {
            List<String> tableList = this.jdbcTemplate.queryForList(sql, new Object[] { tableName }, String.class);
            return CollectionUtils.isNotEmpty(tableList) && tableList.contains(tableName);
        } catch (SQLException e) {
            log.error("sql execute failed:" + sql, e);
            throw e;
        }
    }

    @SneakyThrows
    public boolean queryField(String tableName, String fieldName) {
        if (this.isH2) {
            tableName = tableName.toUpperCase();
            fieldName = fieldName.toUpperCase();
        } else {
            tableName = tableName.toLowerCase();
            fieldName = fieldName.toLowerCase();
        }

        String sql = this.isH2 ? H2_QUERY_COLUMN : MYSQL_QUERY_COLUMN;
        try {
            List<String> tableList = this.jdbcTemplate.queryForList(sql, new Object[] { tableName }, String.class);
            return CollectionUtils.isNotEmpty(tableList) && tableList.contains(fieldName);
        } catch (SQLException e) {
            log.error("sql execute failed:" + sql, e);
            throw e;
        }
    }

    @SneakyThrows
    public List<String> queryField(String tableName) {
        if (this.isH2) {
            tableName = tableName.toUpperCase();
        } else {
            tableName = tableName.toLowerCase();
        }

        String sql = this.isH2 ? H2_QUERY_COLUMN : MYSQL_QUERY_COLUMN;
        try {
            List<String> tableList = this.jdbcTemplate.queryForList(sql, new Object[] { tableName }, String.class);
            return tableList;
        } catch (SQLException e) {
            log.error("sql execute failed:" + sql, e);
            throw e;
        }
    }

    @SneakyThrows
    public Set<String> queryIndexes(String tableName){
        try {
            String sql="show index from "+tableName;
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            return list.stream().map(map -> map.get("Key_name").toString()).filter(str->!str.equals("PRIMARY")).collect(Collectors.toSet());
        } catch (SQLException e) {
            log.error("索引更新失败："+e);
        }
        return null;
    }

    //查询表中是否已经存在数据了,如果存在返回true
    @SneakyThrows
    public boolean queryExist(String tableName, int id) {
        String sql="select 1 from "+tableName+" where id="+id+" limit 1";
        String s = jdbcTemplate.queryForString(sql);
        return StringUtils.isNotBlank(s);
    }
}
