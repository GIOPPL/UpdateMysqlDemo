package com.potato.demo3.config;

import lombok.Getter;
import lombok.Setter;

/**
 * JDBC config and bean
 * @author Ekko
 * @date 2022/12/9 10:08
 */
@Getter
@Setter
public class DsConfig {
    private String driver;
    private String url;
    private String username;
    private String password;
}
