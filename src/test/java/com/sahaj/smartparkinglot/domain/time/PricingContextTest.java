package com.sahaj.smartparkinglot.domain.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingContextTest {
    private final FederalHolidayCalendar calendar =
            date -> date.equals(LocalDate.of(2026, 1, 1));

    @Test
    void defaultsToLosAngelesZoneAndRetainsCalendar() {
        PricingContext context = new PricingContext(calendar);

        assertEquals(ZoneId.of("America/Los_Angeles"), context.zoneId());
        assertSame(calendar, context.holidayCalendar());
        assertTrue(context.holidayCalendar().isHoliday(LocalDate.of(2026, 1, 1)));
        assertFalse(context.holidayCalendar().isHoliday(LocalDate.of(2026, 1, 2)));
    }

    @Test
    void acceptsExplicitFacilityZone() {
        ZoneId zone = ZoneId.of("America/New_York");

        PricingContext context = new PricingContext(zone, calendar);

        assertEquals(zone, context.zoneId());
    }

    @Test
    void requiresZoneAndCalendar() {
        assertThrows(NullPointerException.class, () -> new PricingContext(null, calendar));
        assertThrows(NullPointerException.class, () -> new PricingContext((FederalHolidayCalendar) null));
    }
}
