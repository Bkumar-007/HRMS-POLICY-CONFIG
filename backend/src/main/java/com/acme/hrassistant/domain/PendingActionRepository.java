package com.acme.hrassistant.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingActionRepository extends JpaRepository<PendingAction, String> {

    Optional<PendingAction> findByIdAndEmployeeId(String id, Long employeeId);
}
