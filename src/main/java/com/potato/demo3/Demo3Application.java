package com.potato.demo3;

import com.potato.demo3.init.InitTableAndFiled;
import com.potato.demo3.jdbc.JdbcConnect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;

@SpringBootApplication
public class Demo3Application {

    public static void main(String[] args) {
        Connection conn = new JdbcConnect().getConnection();
        InitTableAndFiled tableAndFiled=new InitTableAndFiled(conn);
        tableAndFiled.scanAndGenerate();
        SpringApplication.run(Demo3Application.class, args);
    }

}
