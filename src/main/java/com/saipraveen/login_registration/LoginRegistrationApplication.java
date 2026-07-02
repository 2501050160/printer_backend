package com.saipraveen.login_registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoginRegistrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoginRegistrationApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner seedAdmin(com.saipraveen.login_registration.repository.AdminRepository adminRepository) {
		return args -> {
			if (adminRepository.count() == 0) {
				com.saipraveen.login_registration.entity.Admin admin = new com.saipraveen.login_registration.entity.Admin();
				admin.setUsername("admin");
				admin.setPassword("admin");
				adminRepository.save(admin);
				System.out.println("Default admin user created: admin / admin");
			}
		};
	}

}
