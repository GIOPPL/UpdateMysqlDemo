package com.potato.demo3.jdbc;

import com.potato.demo3.config.DsConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.hasor.cobble.setting.DefaultSettings;
import net.hasor.cobble.setting.provider.StreamType;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Generate JdbcTemplate
 * @author Ekko
 * @date 2022/12/9 11:09
 */
@Slf4j
public class JdbcConnect {

    @SneakyThrows
    public Connection getConnection() {
        DsConfig properties = getProperties();
        Class.forName(properties.getDriver());
        return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
    }

    @SneakyThrows
    private DsConfig getProperties() {
        DefaultSettings settings = new DefaultSettings();
        settings.addResource("application.yml", StreamType.Yaml);
        settings.refresh();

        DsConfig dsConfig = new DsConfig();
        dsConfig.setDriver(settings.getString("spring.datasource.driverClassName"));
        dsConfig.setUrl(settings.getString("spring.datasource.url"));
        dsConfig.setUsername(settings.getString("spring.datasource.username"));
        dsConfig.setPassword(settings.getString("spring.datasource.password"));
        return dsConfig;
    }
}
