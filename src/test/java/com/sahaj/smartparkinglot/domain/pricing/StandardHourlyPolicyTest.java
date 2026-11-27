package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.Money;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.time.PricingContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.LOS_ANGELES;
import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.context;
import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.stay;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardHourlyPolicyTest {
    private final StandardHourlyPolicy policy = new StandardHourlyPolicy();

    @Test
    void roundsElapsedTimeUpToProgressiveHourlySegments() {
        PricingContext context = context();

        assertEquals(Money.of("5.00"), fare(stay(
                LocalDate.of(2026, 1, 4), LocalTime.NOON,
                LocalDate.of(2026, 1, 4), LocalTime.of(13, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context));
        assertEquals(Money.of("8.00"), fare(stay(
                LocalDate.of(2026, 1, 4), LocalTime.NOON,
                LocalDate.of(2026, 1, 4), LocalTime.of(13, 0).plusNanos(1),
                VehicleType.CAR, LoyaltyTier.NONE), context));
        assertEquals(Money.of("10.00"), fare(stay(
                LocalDate.of(2026, 1, 4), LocalTime.NOON,
                LocalDate.of(2026, 1, 4), LocalTime.of(15, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context));
    }

    @Test
    void chargesWholeSegmentForAnyPeakWindowOverlap() {
        PricingContext context = context();

        assertEquals(Money.of("7.50"), fare(stay(
                LocalDate.of(2026, 1, 5), LocalTime.of(6, 30),
                LocalDate.of(2026, 1, 5), LocalTime.of(7, 30),
                VehicleType.CAR, LoyaltyTier.NONE), context));
        assertEquals(Money.of("7.50"), fare(stay(
                LocalDate.of(2026, 1, 5), LocalTime.of(15, 30),
                LocalDate.of(2026, 1, 5), LocalTime.of(16, 30),
                VehicleType.CAR, LoyaltyTier.NONE), context));
    }

    @Test
    void treatsPeakWindowStartsAsInclusiveAndEndsAsExclusive() {
        PricingContext context = context();

        assertEquals(Money.of("7.50"), fare(stay(
                LocalDate.of(2026, 1, 5), LocalTime.of(7, 0),
                LocalDate.of(2026, 1, 5), LocalTime.of(8, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context));
        assertEquals(Money.of("7.50"), fare(stay(
                LocalDate.of(2026, 1, 5), LocalTime.of(9, 0),
                LocalDate.of(2026, 1, 5), LocalTime.of(10, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context));
        assertEquals(Money.of("5.00"), fare(stay(
                LocalDate.of(2026, 1, 5), LocalTime.of(10, 0),
                LocalDate.of(2026, 1, 5), LocalTime.of(11, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context));
    }

    @Test
    void doesNotSurchargeWeekendsOrFederalHolidays() {
        assertEquals(Money.of("5.00"), fare(stay(
                LocalDate.of(2026, 1, 10), LocalTime.of(7, 0),
                LocalDate.of(2026, 1, 10), LocalTime.of(8, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context()));
        assertEquals(Money.of("5.00"), fare(stay(
                LocalDate.of(2026, 1, 5), LocalTime.of(7, 0),
                LocalDate.of(2026, 1, 5), LocalTime.of(8, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context(LocalDate.of(2026, 1, 5))));
    }

    @Test
    void appliesVehicleMultipliersToHourlyTotals() {
        LocalDate sunday = LocalDate.of(2026, 1, 4);
        assertEquals(Money.of("4.00"), fare(stay(
                sunday, LocalTime.NOON, sunday, LocalTime.of(13, 0),
                VehicleType.MOTORCYCLE, LoyaltyTier.NONE), context()));
        assertEquals(Money.of("10.00"), fare(stay(
                sunday, LocalTime.NOON, sunday, LocalTime.of(13, 0),
                VehicleType.BUS, LoyaltyTier.NONE), context()));
    }

    @Test
    void usesElapsedInstantsAcrossSpringForwardTransition() {
        Instant entry = Instant.parse("2026-03-08T09:30:00Z");
        Instant exit = Instant.parse("2026-03-08T10:30:00Z");
        ParkingStay stay = new ParkingStay(
                UUID.randomUUID(), "TEST-01", VehicleType.CAR, LoyaltyTier.NONE, entry, exit);

        assertEquals(Money.of("5.00"), fare(stay, context()));
        assertEquals(1, stay.elapsedDuration().toHours());
        assertEquals(LocalTime.of(1, 30), entry.atZone(LOS_ANGELES).toLocalTime());
        assertEquals(LocalTime.of(3, 30), exit.atZone(LOS_ANGELES).toLocalTime());
    }

    private Money fare(ParkingStay stay, PricingContext context) {
        return policy.evaluate(stay, context).orElseThrow().exactAmount();
    }
}
