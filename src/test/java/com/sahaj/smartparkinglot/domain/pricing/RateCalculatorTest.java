package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.FareQuote;
import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.Money;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.exception.InvalidPricingConfigurationException;
import com.sahaj.smartparkinglot.domain.time.PricingContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.context;
import static com.sahaj.smartparkinglot.testsupport.PricingTestSupport.stay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateCalculatorTest {
    @Test
    void selectsLowestExactAmountThenRoundsSelectedFareOnce() {
        RateCalculator calculator = calculator(
                fixed(PricingPolicyType.STANDARD_HOURLY, "12.00"),
                fixed(PricingPolicyType.EARLY_BIRD, "10.004"),
                fixed(PricingPolicyType.NIGHT_OWL, "10.003"));

        FareQuote quote = calculator.calculate(validStay());

        assertEquals(PricingPolicyType.NIGHT_OWL, quote.policy());
        assertEquals(Money.of("10.003"), quote.exactAmount());
        assertEquals(Money.of("10.00"), quote.amount());
    }

    @Test
    void usesEarlyBirdThenNightOwlThenStandardForExactTies() {
        ParkingStay stay = validStay();
        assertEquals(PricingPolicyType.EARLY_BIRD, calculator(
                fixed(PricingPolicyType.STANDARD_HOURLY, "10"),
                fixed(PricingPolicyType.EARLY_BIRD, "10"),
                fixed(PricingPolicyType.NIGHT_OWL, "10")).calculate(stay).policy());

        assertEquals(PricingPolicyType.NIGHT_OWL, calculator(
                fixed(PricingPolicyType.STANDARD_HOURLY, "10"),
                empty(),
                fixed(PricingPolicyType.NIGHT_OWL, "10")).calculate(stay).policy());
    }

    @Test
    void excludesBothSpecialPoliciesForStaysLongerThan24Hours() {
        AtomicInteger earlyBirdCalls = new AtomicInteger();
        AtomicInteger nightOwlCalls = new AtomicInteger();
        RateCalculator calculator = calculator(
                fixed(PricingPolicyType.STANDARD_HOURLY, "20"),
                counting(PricingPolicyType.EARLY_BIRD, earlyBirdCalls),
                counting(PricingPolicyType.NIGHT_OWL, nightOwlCalls));
        Instant entry = Instant.parse("2026-01-04T20:00:00Z");
        ParkingStay longStay = new ParkingStay(
                UUID.randomUUID(), "TEST-01", VehicleType.CAR, LoyaltyTier.NONE,
                entry, entry.plus(Duration.ofHours(24)).plusNanos(1));

        FareQuote quote = calculator.calculate(longStay);

        assertEquals(PricingPolicyType.STANDARD_HOURLY, quote.policy());
        assertEquals(0, earlyBirdCalls.get());
        assertEquals(0, nightOwlCalls.get());
    }

    @Test
    void treatsStandardHourlyAsRequiredAndRejectsMismatchedPolicyResults() {
        RateCalculator missingStandard = calculator(
                empty(), empty(), empty());
        assertThrows(InvalidPricingConfigurationException.class,
                () -> missingStandard.calculate(validStay()));

        RateCalculator mismatched = calculator(
                fixed(PricingPolicyType.NIGHT_OWL, "5"), empty(), empty());
        assertThrows(InvalidPricingConfigurationException.class,
                () -> mismatched.calculate(validStay()));
    }

    @Test
    void selectsEarlyBirdOverHigherStandardFareForAnApplicableStay() {
        PricingContext context = new PricingContext(
                com.sahaj.smartparkinglot.testsupport.PricingTestSupport.LOS_ANGELES,
                date -> false);
        RateCalculator calculator = new RateCalculator(
                context,
                new StandardHourlyPolicy(),
                new EarlyBirdPolicy(),
                new NightOwlPolicy());
        ParkingStay stay = stay(
                LocalDate.of(2026, 1, 5), LocalTime.of(6, 30),
                LocalDate.of(2026, 1, 5), LocalTime.of(16, 0),
                VehicleType.CAR, LoyaltyTier.NONE);

        FareQuote quote = calculator.calculate(stay);

        assertEquals(PricingPolicyType.EARLY_BIRD, quote.policy());
        assertEquals(Money.of("15.00"), quote.amount());
    }

    private static RateCalculator calculator(
            RatePolicy standardHourly, RatePolicy earlyBird, RatePolicy nightOwl) {
        return new RateCalculator(context(), standardHourly, earlyBird, nightOwl);
    }

    private static RatePolicy fixed(PricingPolicyType type, String amount) {
        return (stay, context) -> Optional.of(new FareCandidate(type, Money.of(amount)));
    }

    private static RatePolicy empty() {
        return (stay, context) -> Optional.empty();
    }

    private static RatePolicy counting(PricingPolicyType type, AtomicInteger calls) {
        return (stay, context) -> {
            calls.incrementAndGet();
            return Optional.of(new FareCandidate(type, Money.of("1")));
        };
    }

    private static ParkingStay validStay() {
        return stay(
                java.time.LocalDate.of(2026, 1, 4), java.time.LocalTime.NOON,
                java.time.LocalDate.of(2026, 1, 4), java.time.LocalTime.of(13, 0),
                VehicleType.CAR, LoyaltyTier.NONE);
    }
}
