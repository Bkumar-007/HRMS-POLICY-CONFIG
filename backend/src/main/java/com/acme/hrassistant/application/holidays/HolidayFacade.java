package com.acme.hrassistant.application.holidays;

import com.acme.hrassistant.domain.Holiday;
import com.acme.hrassistant.mockhr.HolidayService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application entry for holiday reads. Mock HR stays in {@code mockhr}.
 */
@Service
public class HolidayFacade {

    private final HolidayService holidayService;

    public HolidayFacade(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    public List<Holiday> getHolidays(int year, Integer month, String employeeLocation) {
        return holidayService.getHolidays(year, month, employeeLocation);
    }
}
