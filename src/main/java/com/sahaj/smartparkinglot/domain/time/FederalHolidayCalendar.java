package com.sahaj.smartparkinglot.domain.time;

import java.time.LocalDate;

@FunctionalInterface
public interface FederalHolidayCalendar {
    boolean isHoliday(LocalDate date);
}
