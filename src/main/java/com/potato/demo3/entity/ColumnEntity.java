package com.potato.demo3.entity;

import lombok.Getter;
import lombok.Setter;

/**
* @author Ekko
* @date 2022/12/12 15:21
*/
@Getter
@Setter
public class ColumnEntity {

    private String  columnName;

    private boolean primaryKey;

    private boolean autoincrement;

    private boolean zeroFill;

    private boolean unsigned;

    private String  comment;

    private String  defaultValue;

    private boolean notNull;

    private String  sqlType;
}
