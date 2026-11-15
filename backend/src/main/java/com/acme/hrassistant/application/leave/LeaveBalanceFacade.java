package com.acme.hrassistant.application.leave;

import com.acme.hrassistant.domain.LeaveBalance;
import com.acme.hrassistant.mockhr.LeaveBalanceService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application entry for leave-balance reads. Mock HR stays in {@code mockhr}.
 */
@Service
public class LeaveBalanceFacade {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceFacade(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    public List<LeaveBalance> getBalancesForEmployee(Long employeeId) {
        return leaveBalanceService.getBalancesForEmployee(employeeId);
    }
}
