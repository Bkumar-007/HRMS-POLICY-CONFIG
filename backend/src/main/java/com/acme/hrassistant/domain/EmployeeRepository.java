package com.acme.hrassistant.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);

    List<Employee> findByManagerIdOrderByDisplayNameAsc(Long managerId);

    List<Employee> findAllByOrderByDisplayNameAsc();
}
