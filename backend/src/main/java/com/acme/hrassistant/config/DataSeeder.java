package com.acme.hrassistant.config;

import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.Holiday;
import com.acme.hrassistant.domain.HolidayRepository;
import com.acme.hrassistant.domain.LeaveBalance;
import com.acme.hrassistant.domain.LeaveBalanceRepository;
import com.acme.hrassistant.domain.LeaveType;
import com.acme.hrassistant.domain.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
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
    CommandLineRunner seedDemoData(
            EmployeeRepository employeeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            HolidayRepository holidayRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            seedEmployees(employeeRepository, passwordEncoder);
            seedLeaveBalances(employeeRepository, leaveBalanceRepository);
            seedHolidays(holidayRepository);
        };
    }

    private void seedEmployees(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        String hash = passwordEncoder.encode("password");
        ensureEmployee(employeeRepository, "priya.employee", hash, "Priya Sharma", Role.EMPLOYEE, "G2", "Pune");
        ensureEmployee(employeeRepository, "rahul.employee", hash, "Rahul Mehta", Role.EMPLOYEE, "G3", "Mumbai");
        ensureEmployee(employeeRepository, "admin.hr", hash, "Asha HR Admin", Role.HR_ADMIN, "G4", "Pune");
        log.info("Demo users ready: priya.employee / rahul.employee / admin.hr (password: password)");
    }

    private void ensureEmployee(
            EmployeeRepository employeeRepository,
            String username,
            String hash,
            String displayName,
            Role role,
            String grade,
            String location) {
        if (employeeRepository.findByUsername(username).isPresent()) {
            return;
        }
        employeeRepository.save(new Employee(username, hash, displayName, role, grade, location));
        log.info("Seeded user {}", username);
    }

    private void seedLeaveBalances(EmployeeRepository employeeRepository, LeaveBalanceRepository leaveBalanceRepository) {
        int year = Year.now().getValue();
        employeeRepository.findAll().forEach(employee -> {
            if (leaveBalanceRepository.existsByEmployeeId(employee.getId())) {
                return;
            }
            // Slightly different balances so demos look realistic
            if ("priya.employee".equals(employee.getUsername())) {
                saveBalances(leaveBalanceRepository, employee.getId(), year, "8.0", "10.0", "12.0");
            } else if ("rahul.employee".equals(employee.getUsername())) {
                saveBalances(leaveBalanceRepository, employee.getId(), year, "5.0", "7.0", "15.0");
            } else {
                saveBalances(leaveBalanceRepository, employee.getId(), year, "12.0", "10.0", "15.0");
            }
        });
        log.info("Ensured leave balances for year {}", year);
    }

    private void saveBalances(
            LeaveBalanceRepository repo, Long employeeId, int year, String cl, String sl, String el) {
        repo.save(new LeaveBalance(employeeId, LeaveType.CASUAL, year, new BigDecimal(cl)));
        repo.save(new LeaveBalance(employeeId, LeaveType.SICK, year, new BigDecimal(sl)));
        repo.save(new LeaveBalance(employeeId, LeaveType.EARNED, year, new BigDecimal(el)));
    }

    private void seedHolidays(HolidayRepository holidayRepository) {
        if (holidayRepository.count() > 0) {
            return;
        }
        List<Holiday> holidays = List.of(
                new Holiday(LocalDate.of(2026, 1, 26), "Republic Day", null),
                new Holiday(LocalDate.of(2026, 3, 3), "Holi", null),
                new Holiday(LocalDate.of(2026, 3, 26), "Good Friday", null),
                new Holiday(LocalDate.of(2026, 5, 1), "Maharashtra Day", "Pune"),
                new Holiday(LocalDate.of(2026, 5, 1), "Maharashtra Day", "Mumbai"),
                new Holiday(LocalDate.of(2026, 8, 15), "Independence Day", null),
                new Holiday(LocalDate.of(2026, 10, 2), "Gandhi Jayanti", null),
                new Holiday(LocalDate.of(2026, 10, 20), "Dussehra", null),
                new Holiday(LocalDate.of(2026, 11, 8), "Diwali", null),
                new Holiday(LocalDate.of(2026, 11, 9), "Diwali (day 2)", null),
                new Holiday(LocalDate.of(2026, 12, 25), "Christmas", null));
        holidayRepository.saveAll(holidays);
        log.info("Seeded {} holidays for 2026", holidays.size());
    }
}
