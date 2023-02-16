package com.potato.demo3.init;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Ekko
 * @date 2022/12/13 15:39
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnDescribe {

    /** Column name */
    String value() default "";

    /** Column remarks */
    String comment() default "";

    /** Column data type */
    String type() default "";

    int length() default -1;

    /** Default value on column */
    String defaultValue() default "";

    /** Indicates whether the column can be empty */
    boolean isNotNull() default true;

    /** Value zerofill */
    boolean isZeroFill() default false;

    /** Value unsigned */
    boolean isUnsigned() default false;

    //该列是否存在，默认为存在
    boolean exist() default true;

}
