package com.acme.hrassistant.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyBreachRepository extends JpaRepository<PolicyBreach, Long> {
    List<PolicyBreach> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<PolicyBreach> findByEmployeeIdInOrderByCreatedAtDesc(List<Long> employeeIds);

    List<PolicyBreach> findAllByOrderByCreatedAtDesc();

    long countByEmployeeIdAndStatus(Long employeeId, PolicyBreachStatus status);
}
