package com.acme.hrassistant.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);

    boolean existsByEmployeeId(Long employeeId);
}
