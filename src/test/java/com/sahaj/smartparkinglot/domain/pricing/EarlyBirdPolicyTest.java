package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.Money;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.time.PricingContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.context;
import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.stay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarlyBirdPolicyTest {
    private final EarlyBirdPolicy policy = new EarlyBirdPolicy();
    private final LocalDate monday = LocalDate.of(2026, 1, 5);

    @Test
    void acceptsInclusiveStartBoundaries() {
        assertEquals(Money.of("15.00"), fare(stay(
                monday, LocalTime.of(6, 0), monday, LocalTime.of(15, 30),
                VehicleType.CAR, LoyaltyTier.NONE), context()));
    }

    @Test
    void rejectsExclusiveAndOutOfWindowBoundaries() {
        assertNotApplicable(LocalTime.of(9, 0), LocalTime.of(16, 0), context());
        assertNotApplicable(LocalTime.of(8, 59, 59), LocalTime.of(19, 0), context());
        assertNotApplicable(LocalTime.of(5, 59, 59), LocalTime.of(16, 0), context());
        assertNotApplicable(LocalTime.of(6, 0), LocalTime.of(15, 29, 59), context());
    }

    @Test
    void requiresSameEligibleWeekdayAndRejectsHolidays() {
        assertTrue(policy.evaluate(stay(
                monday, LocalTime.of(6, 0), monday.plusDays(1), LocalTime.of(5, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context()).isEmpty());
        assertNotApplicable(LocalTime.of(6, 0), LocalTime.of(16, 0),
                context(monday));
        assertTrue(policy.evaluate(stay(
                LocalDate.of(2026, 1, 10), LocalTime.of(6, 0),
                LocalDate.of(2026, 1, 10), LocalTime.of(16, 0),
                VehicleType.CAR, LoyaltyTier.NONE), context()).isEmpty());
    }

    @Test
    void rejectsStaysLongerThanTwentyFourHours() {
        assertTrue(policy.evaluate(stay(
                monday, LocalTime.of(6, 0), monday.plusDays(1), LocalTime.of(6, 1),
                VehicleType.CAR, LoyaltyTier.NONE), context()).isEmpty());
    }

    @Test
    void appliesLoyaltyDiscountBeforeVehicleMultiplier() {
        Map<LoyaltyTier, Money> expected = Map.of(
                LoyaltyTier.NONE, Money.of("15.00"),
                LoyaltyTier.SILVER, Money.of("13.50"),
                LoyaltyTier.GOLD, Money.of("12.00"),
                LoyaltyTier.PLATINUM, Money.of("10.50"));
        for (Map.Entry<LoyaltyTier, Money> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), fare(stay(
                    monday, LocalTime.of(6, 30), monday, LocalTime.of(16, 0),
                    VehicleType.CAR, entry.getKey()), context()));
        }
        assertEquals(Money.of("21.00"), fare(stay(
                monday, LocalTime.of(6, 30), monday, LocalTime.of(16, 0),
                VehicleType.BUS, LoyaltyTier.PLATINUM), context()));
    }

    private Money fare(ParkingStay stay, PricingContext context) {
        FareCandidate candidate = policy.evaluate(stay, context).orElseThrow();
        assertEquals(PricingPolicyType.EARLY_BIRD, candidate.policy());
        return candidate.exactAmount();
    }

    private void assertNotApplicable(LocalTime entryTime, LocalTime exitTime, PricingContext context) {
        Optional<FareCandidate> candidate = policy.evaluate(stay(
                monday, entryTime, monday, exitTime, VehicleType.CAR, LoyaltyTier.NONE), context);
        assertTrue(candidate.isEmpty());
    }
}
