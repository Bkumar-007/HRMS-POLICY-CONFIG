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
import org.springframework.util.StringUtils;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Fictional contacts for priya.employee and admin.hr (not real PII). */
    private static final String OWNER_EMAIL = "priya.sharma@acme.example";

    private static final String OWNER_PHONE = "9876543210";

    /** Placeholder contacts for other demo users (not real). */
    private static final String OTHER_EMAIL = "demo.employee@acme.example";

    private static final String OTHER_PHONE = "9000000000";

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

        ensureEmployee(
                employeeRepository,
                "admin.hr",
                hash,
                "Asha HR Admin",
                Role.HR_ADMIN,
                "G4",
                "Pune",
                "Human Resources",
                null);
        ensureEmployee(
                employeeRepository,
                "neha.manager",
                hash,
                "Neha Kapoor",
                Role.MANAGER,
                "G4",
                "Pune",
                "Engineering",
                null);
        ensureEmployee(
                employeeRepository,
                "vikram.manager",
                hash,
                "Vikram Singh",
                Role.MANAGER,
                "G4",
                "Mumbai",
                "Operations",
                null);

        Long nehaId = employeeRepository.findByUsername("neha.manager").map(Employee::getId).orElse(null);
        Long vikramId = employeeRepository.findByUsername("vikram.manager").map(Employee::getId).orElse(null);

        ensureEmployee(
                employeeRepository,
                "priya.employee",
                hash,
                "Priya Sharma",
                Role.EMPLOYEE,
                "G2",
                "Pune",
                "Engineering",
                nehaId);
        ensureEmployee(
                employeeRepository,
                "amit.employee",
                hash,
                "Amit Joshi",
                Role.EMPLOYEE,
                "G2",
                "Pune",
                "Engineering",
                nehaId);
        ensureEmployee(
                employeeRepository,
                "rahul.employee",
                hash,
                "Rahul Mehta",
                Role.EMPLOYEE,
                "G3",
                "Mumbai",
                "Operations",
                vikramId);
        ensureEmployee(
                employeeRepository,
                "sara.employee",
                hash,
                "Sara Khan",
                Role.EMPLOYEE,
                "G3",
                "Mumbai",
                "Operations",
                vikramId);

        backfillContactsAndOrg(employeeRepository, nehaId, vikramId);
        log.info(
                "Demo users ready: admin.hr / neha.manager / vikram.manager / priya.employee / amit.employee / rahul.employee / sara.employee (password: password)");
        log.info("OTP contacts for priya.employee & admin.hr: {} / {}", OWNER_EMAIL, OWNER_PHONE);
    }

    private void ensureEmployee(
            EmployeeRepository employeeRepository,
            String username,
            String hash,
            String displayName,
            Role role,
            String grade,
            String location,
            String department,
            Long managerId) {
        if (employeeRepository.findByUsername(username).isPresent()) {
            return;
        }
        String email = isOwnerAccount(username) ? OWNER_EMAIL : OTHER_EMAIL;
        String phone = isOwnerAccount(username) ? OWNER_PHONE : OTHER_PHONE;
        employeeRepository.save(new Employee(
                username, hash, displayName, role, grade, location, email, phone, department, managerId));
        log.info("Seeded user {}", username);
    }

    private static boolean isOwnerAccount(String username) {
        return "priya.employee".equals(username) || "admin.hr".equals(username);
    }

    private void backfillContactsAndOrg(
            EmployeeRepository employeeRepository, Long nehaId, Long vikramId) {
        employeeRepository.findAll().forEach(employee -> {
            boolean dirty = false;
            // Always keep seeded demo accounts on fictional contacts (never real PII).
            String expectedEmail = isOwnerAccount(employee.getUsername()) ? OWNER_EMAIL : OTHER_EMAIL;
            String expectedPhone = isOwnerAccount(employee.getUsername()) ? OWNER_PHONE : OTHER_PHONE;
            if (!expectedEmail.equals(employee.getEmail())) {
                employee.setEmail(expectedEmail);
                dirty = true;
            }
            if (!expectedPhone.equals(employee.getPhone())) {
                employee.setPhone(expectedPhone);
                dirty = true;
            }
            if (!StringUtils.hasText(employee.getDepartment())) {
                if (employee.getUsername().contains("manager") || "priya.employee".equals(employee.getUsername())
                        || "amit.employee".equals(employee.getUsername())) {
                    employee.setDepartment("Engineering".equals(guessDept(employee.getUsername()))
                            ? "Engineering"
                            : guessDept(employee.getUsername()));
                } else {
                    employee.setDepartment(guessDept(employee.getUsername()));
                }
                dirty = true;
            }
            if (employee.getManagerId() == null) {
                if ("priya.employee".equals(employee.getUsername()) || "amit.employee".equals(employee.getUsername())) {
                    employee.setManagerId(nehaId);
                    dirty = true;
                } else if ("rahul.employee".equals(employee.getUsername())
                        || "sara.employee".equals(employee.getUsername())) {
                    employee.setManagerId(vikramId);
                    dirty = true;
                }
            }
            // Promote existing neha/vikram if re-seeded as employees somehow — ensure manager demos exist
            if ("neha.manager".equals(employee.getUsername()) && employee.getRole() == Role.EMPLOYEE) {
                employee.setRole(Role.MANAGER);
                dirty = true;
            }
            if ("vikram.manager".equals(employee.getUsername()) && employee.getRole() == Role.EMPLOYEE) {
                employee.setRole(Role.MANAGER);
                dirty = true;
            }
            if (dirty) {
                employeeRepository.save(employee);
            }
        });
    }

    private static String guessDept(String username) {
        if (username == null) {
            return "General";
        }
        if (username.contains("hr") || username.startsWith("admin")) {
            return "Human Resources";
        }
        if (username.contains("rahul") || username.contains("sara") || username.contains("vikram")) {
            return "Operations";
        }
        return "Engineering";
    }

    private void seedLeaveBalances(EmployeeRepository employeeRepository, LeaveBalanceRepository leaveBalanceRepository) {
        int year = Year.now().getValue();
        employeeRepository.findAll().forEach(employee -> {
            if (leaveBalanceRepository.existsByEmployeeId(employee.getId())) {
                return;
            }
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
                new Holiday(LocalDate.of(2026, 12, 25), "Christmas", null));
        holidayRepository.saveAll(holidays);
        log.info("Seeded {} holidays", holidays.size());
    }
}
