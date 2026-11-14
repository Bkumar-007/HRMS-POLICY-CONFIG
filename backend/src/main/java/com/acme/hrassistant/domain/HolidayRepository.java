package com.acme.hrassistant.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    @Query("""
            select h from Holiday h
            where h.date >= :from and h.date < :to
              and (h.location is null or h.location = '' or h.location = :location)
            order by h.date asc
            """)
    List<Holiday> findForRangeAndLocation(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("location") String location);
}
