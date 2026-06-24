package com.saipraveen.login_registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoginRegistrationApplication {

	public static void main(String[] args) {
		String databaseUrl = System.getenv("DATABASE_URL");
		if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
			try {
				String cleanUrl = databaseUrl.substring("postgresql://".length());
				int atIndex = cleanUrl.indexOf("@");
				if (atIndex != -1) {
					String credentials = cleanUrl.substring(0, atIndex);
					String rest = cleanUrl.substring(atIndex + 1);
					
					String[] credParts = credentials.split(":", 2);
					String username = credParts[0];
					String password = credParts.length > 1 ? credParts[1] : "";
					
					String jdbcUrl = "jdbc:postgresql://" + rest;
					
					System.setProperty("spring.datasource.url", jdbcUrl);
					System.setProperty("spring.datasource.username", username);
					System.setProperty("spring.datasource.password", password);
					
					System.out.println("Parsed DATABASE_URL to JDBC: " + jdbcUrl);
				}
			} catch (Exception e) {
				System.err.println("Failed to parse DATABASE_URL: " + e.getMessage());
			}
		}
		SpringApplication.run(LoginRegistrationApplication.class, args);
	}

}
