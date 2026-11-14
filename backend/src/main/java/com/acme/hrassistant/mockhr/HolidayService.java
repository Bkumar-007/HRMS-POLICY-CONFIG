package com.acme.hrassistant.mockhr;

import com.acme.hrassistant.domain.Holiday;
import com.acme.hrassistant.domain.HolidayRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Transactional(readOnly = true)
    public List<Holiday> getHolidays(int year, Integer month, String employeeLocation) {
        LocalDate from;
        LocalDate to;
        if (month != null && month >= 1 && month <= 12) {
            from = LocalDate.of(year, month, 1);
            to = from.plusMonths(1);
        } else {
            from = LocalDate.of(year, 1, 1);
            to = LocalDate.of(year + 1, 1, 1);
        }
        return holidayRepository.findForRangeAndLocation(from, to, employeeLocation);
    }
}
