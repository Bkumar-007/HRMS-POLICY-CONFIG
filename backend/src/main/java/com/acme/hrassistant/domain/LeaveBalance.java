package com.acme.hrassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(
        name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type", "year"}))
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal remainingDays;

    protected LeaveBalance() {
    }

    public LeaveBalance(Long employeeId, LeaveType leaveType, int year, BigDecimal remainingDays) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.year = year;
        this.remainingDays = remainingDays;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public int getYear() {
        return year;
    }

    public BigDecimal getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(BigDecimal remainingDays) {
        this.remainingDays = remainingDays;
    }
}
