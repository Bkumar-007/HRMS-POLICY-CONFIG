package com.acme.hrassistant.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Optional<LeaveRequest> findByPendingActionId(String pendingActionId);

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<LeaveRequest> findByEmployeeIdInOrderByCreatedAtDesc(Collection<Long> employeeIds);

    List<LeaveRequest> findByStatusInOrderByCreatedAtDesc(Collection<LeaveRequestStatus> statuses);

    List<LeaveRequest> findByEmployeeIdInAndStatusInOrderByCreatedAtDesc(
            Collection<Long> employeeIds, Collection<LeaveRequestStatus> statuses);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
}
