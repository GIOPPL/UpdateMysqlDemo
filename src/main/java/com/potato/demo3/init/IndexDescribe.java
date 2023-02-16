package com.potato.demo3.init;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(IndexDescribes.class)
public @interface IndexDescribe{
    //索引列
    String[] value() ;

    //索引名
    String name() ;

    //是否是唯一索引
    boolean unique() default false;
}
