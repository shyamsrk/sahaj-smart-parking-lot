package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.Money;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.context;
import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.stay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NightOwlPolicyTest {
    private final NightOwlPolicy policy = new NightOwlPolicy();
    private final LocalDate monday = LocalDate.of(2026, 1, 5);

    @Test
    void acceptsInclusiveEntryAndExitStartsOnConsecutiveEligibleWeekdays() {
        FareCandidate candidate = policy.evaluate(stay(
                monday, LocalTime.of(18, 0), monday.plusDays(1), LocalTime.of(5, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context()).orElseThrow();

        assertEquals(PricingPolicyType.NIGHT_OWL, candidate.policy());
        assertEquals(Money.of("8.00"), candidate.exactAmount());
    }

    @Test
    void rejectsExclusiveExitEndAndInvalidDateOrTimeWindows() {
        assertNotApplicable(monday, LocalTime.of(17, 59, 59),
                monday.plusDays(1), LocalTime.of(6, 0), context());
        assertNotApplicable(monday, LocalTime.of(18, 0),
                monday.plusDays(1), LocalTime.of(10, 0), context());
        assertNotApplicable(monday, LocalTime.of(18, 0),
                monday.plusDays(2), LocalTime.of(6, 0), context());
        assertNotApplicable(monday, LocalTime.of(18, 0),
                monday.plusDays(1), LocalTime.of(4, 59, 59), context());
    }

    @Test
    void requiresBothDatesToBeEligibleWeekdays() {
        assertNotApplicable(LocalDate.of(2026, 1, 9), LocalTime.of(18, 0),
                LocalDate.of(2026, 1, 10), LocalTime.of(6, 0), context());
        assertNotApplicable(monday, LocalTime.of(18, 0),
                monday.plusDays(1), LocalTime.of(6, 0), context(monday.plusDays(1)));
        assertNotApplicable(LocalDate.of(2026, 1, 11), LocalTime.of(18, 0),
                LocalDate.of(2026, 1, 12), LocalTime.of(6, 0), context());
    }

    @Test
    void appliesLoyaltyDiscountAndVehicleMultiplier() {
        assertEquals(Money.of("6.40"), policy.evaluate(stay(
                monday, LocalTime.of(18, 30), monday.plusDays(1), LocalTime.of(6, 0),
                VehicleType.CAR, LoyaltyTier.GOLD), context()).orElseThrow().exactAmount());
        assertEquals(Money.of("16.00"), policy.evaluate(stay(
                monday, LocalTime.of(18, 30), monday.plusDays(1), LocalTime.of(6, 0),
                VehicleType.BUS, LoyaltyTier.NONE), context()).orElseThrow().exactAmount());
    }

    private void assertNotApplicable(
            LocalDate entryDate,
            LocalTime entryTime,
            LocalDate exitDate,
            LocalTime exitTime,
            com.sahaj.smartparkinglot.domain.time.PricingContext context) {
        ParkingStay stay = stay(
                entryDate, entryTime, exitDate, exitTime, VehicleType.CAR, LoyaltyTier.NONE);
        assertTrue(policy.evaluate(stay, context).isEmpty());
    }
}
