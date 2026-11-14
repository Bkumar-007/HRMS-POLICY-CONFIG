package com.acme.hrassistant.config;

import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedEmployees(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (employeeRepository.count() > 0) {
                return;
            }
            String hash = passwordEncoder.encode("password");
            employeeRepository.save(new Employee(
                    "priya.employee", hash, "Priya Sharma", Role.EMPLOYEE, "G2", "Pune"));
            employeeRepository.save(new Employee(
                    "admin.hr", hash, "Asha HR Admin", Role.HR_ADMIN, "G4", "Pune"));
            log.info("Seeded demo users: priya.employee / admin.hr (password: password)");
        };
    }
}