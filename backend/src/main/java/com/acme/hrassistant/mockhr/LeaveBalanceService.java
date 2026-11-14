package com.acme.hrassistant.mockhr;

import com.acme.hrassistant.domain.LeaveBalance;
import com.acme.hrassistant.domain.LeaveBalanceRepository;
import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalance> getBalancesForEmployee(Long employeeId) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, Year.now().getValue());
    }
}
