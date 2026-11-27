package com.sahaj.smartparkinglot.domain.time;

import java.time.ZoneId;
import java.util.Objects;

public record PricingContext(ZoneId zoneId, FederalHolidayCalendar holidayCalendar) {
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Los_Angeles");

    public PricingContext {
        Objects.requireNonNull(zoneId, "Facility zone is required");
        Objects.requireNonNull(holidayCalendar, "Holiday calendar is required");
    }

    public PricingContext(FederalHolidayCalendar holidayCalendar) {
        this(DEFAULT_ZONE, holidayCalendar);
    }
}
