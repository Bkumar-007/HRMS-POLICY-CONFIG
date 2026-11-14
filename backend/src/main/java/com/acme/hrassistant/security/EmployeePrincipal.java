package com.acme.hrassistant.security;

import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class EmployeePrincipal implements UserDetails {

    private final Long employeeId;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final Role role;
    private final String grade;
    private final String location;

    public EmployeePrincipal(Employee employee) {
        this.employeeId = employee.getId();
        this.username = employee.getUsername();
        this.passwordHash = employee.getPasswordHash();
        this.displayName = employee.getDisplayName();
        this.role = employee.getRole();
        this.grade = employee.getGrade();
        this.location = employee.getLocation();
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public String getGrade() {
        return grade;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}