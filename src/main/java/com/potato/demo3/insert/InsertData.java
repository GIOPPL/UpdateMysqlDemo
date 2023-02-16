package com.potato.demo3.insert;

import lombok.Data;

@Data
public class InsertData {
    private String tableName;
    private int id;
    private String sql;

    public InsertData(String tableName, int id, String sql) {
        this.tableName = tableName;
        this.id = id;
        this.sql = sql;
    }

    public static final InsertData[] SQL_LIST={
            new InsertData("student2",1000,"insert into student2(id,name,sex,mm)values(1000,'李','2022-10-10 12:30:20',12.34)"),
            new InsertData("student2",1001,"insert into student2(id,name,mm)values(1001,'王',12.34)"),
            new InsertData("student2",1,"insert into student2(id,name,mm)values(1001,'王',12.34)"),
    };


}
