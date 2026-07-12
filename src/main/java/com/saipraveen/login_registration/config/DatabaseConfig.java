package com.saipraveen.login_registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;
import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:postgresql://dpg-d8ttgrnavr4c73a22jsg-a.oregon-postgres.render.com:5432/printer_db_60ga")
                .username("printer_db_60ga_user")
                .password("TiKy13IFab7aXV3nvlhKkqImMROtDvbl")
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
