package com.potato.demo3.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.potato.demo3.init.ColumnDescribe;
import com.potato.demo3.init.IndexDescribe;

@TableName(value = "student2")
@IndexDescribe(name="index_id_name_sex", value = {"id","name","sex"},unique = false)
@IndexDescribe(name = "index_name",value = {"name(13)"})
public class StudentEntity {

//    @ColumnDescribe(value = "stu_id" ,type = "int")
    @TableId(type = IdType.AUTO)
    private int id;

//    @ColumnDescribe(value = "stu_name",type = "varchar(10)")
    @ColumnDescribe(length = 123)
    private String name;

//    @ColumnDescribe(value = "stu_sex",type = "varchar(10)")
    @ColumnDescribe(type = "datetime",isNotNull = false)
    private String sex;

    @ColumnDescribe(type = "decimal")
    private Double mm;
}
