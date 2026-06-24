package com.saipraveen.login_registration.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfig {

    @Autowired
    private Environment env;

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || !databaseUrl.startsWith("postgresql://")) {
            String springUrl = env.getProperty("spring.datasource.url");
            if (springUrl != null && springUrl.startsWith("postgresql://")) {
                databaseUrl = springUrl;
            }
        }
        
        String jdbcUrl = null;
        String username = null;
        String password = null;

        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            try {
                String cleanUrl = databaseUrl.substring("postgresql://".length());
                int atIndex = cleanUrl.indexOf("@");
                if (atIndex != -1) {
                    String credentials = cleanUrl.substring(0, atIndex);
                    String rest = cleanUrl.substring(atIndex + 1);
                    
                    String[] credParts = credentials.split(":", 2);
                    username = credParts[0];
                    password = credParts.length > 1 ? credParts[1] : "";
                    
                    jdbcUrl = "jdbc:postgresql://" + rest;
                }
            } catch (Exception e) {
                System.err.println("Failed to parse DATABASE_URL dynamically: " + e.getMessage());
            }
        }

        if (jdbcUrl == null) {
            jdbcUrl = env.getProperty("spring.datasource.url");
            username = env.getProperty("spring.datasource.username");
            password = env.getProperty("spring.datasource.password");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        
        String driver = env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
        config.setDriverClassName(driver);

        System.out.println("Initializing HikariDataSource with URL: " + jdbcUrl);
        return new HikariDataSource(config);
    }
}
