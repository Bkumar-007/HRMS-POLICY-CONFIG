package com.acme.hrassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "holidays",
        uniqueConstraints = @UniqueConstraint(columnNames = {"holiday_date", "location"}))
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Null or blank means company-wide (all locations).
     */
    @Column(length = 40)
    private String location;

    protected Holiday() {
    }

    public Holiday(LocalDate date, String name, String location) {
        this.date = date;
        this.name = name;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }
}
