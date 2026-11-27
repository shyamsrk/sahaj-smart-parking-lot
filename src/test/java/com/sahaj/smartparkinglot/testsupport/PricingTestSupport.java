package com.sahaj.smartparkinglot.testsupport;

import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.time.PricingContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PricingTestSupport {
    public static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");

    private PricingTestSupport() {
    }

    public static PricingContext context(LocalDate... holidays) {
        Set<LocalDate> holidayDates = new HashSet<>(Arrays.asList(holidays));
        return new PricingContext(LOS_ANGELES, holidayDates::contains);
    }

    public static ParkingStay stay(
            LocalDate entryDate,
            LocalTime entryTime,
            LocalDate exitDate,
            LocalTime exitTime,
            VehicleType vehicleType,
            LoyaltyTier loyaltyTier) {
        Instant entryAt = entryDate.atTime(entryTime).atZone(LOS_ANGELES).toInstant();
        Instant exitAt = exitDate.atTime(exitTime).atZone(LOS_ANGELES).toInstant();
        return new ParkingStay(
                UUID.randomUUID(), "TEST-01", vehicleType, loyaltyTier, entryAt, exitAt);
    }
}
