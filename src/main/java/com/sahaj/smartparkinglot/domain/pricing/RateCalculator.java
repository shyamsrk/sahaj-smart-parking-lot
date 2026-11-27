package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.FareQuote;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.exception.InvalidPricingConfigurationException;
import com.sahaj.smartparkinglot.domain.time.PricingContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RateCalculator implements FareCalculator {
    private static final Duration MAX_SPECIAL_STAY = Duration.ofHours(24);

    private final PricingContext context;
    private final RatePolicy standardHourlyPolicy;
    private final RatePolicy earlyBirdPolicy;
    private final RatePolicy nightOwlPolicy;

    public RateCalculator(
            PricingContext context,
            RatePolicy standardHourlyPolicy,
            RatePolicy earlyBirdPolicy,
            RatePolicy nightOwlPolicy) {
        this.context = Objects.requireNonNull(context, "Pricing context is required");
        this.standardHourlyPolicy = Objects.requireNonNull(
                standardHourlyPolicy, "Standard hourly policy is required");
        this.earlyBirdPolicy = Objects.requireNonNull(earlyBirdPolicy, "Early Bird policy is required");
        this.nightOwlPolicy = Objects.requireNonNull(nightOwlPolicy, "Night Owl policy is required");
    }

    @Override
    public FareQuote calculate(ParkingStay stay) {
        Objects.requireNonNull(stay, "Parking stay is required");
        List<FareCandidate> candidates = new ArrayList<>();
        candidates.addAll(evaluate(standardHourlyPolicy, PricingPolicyType.STANDARD_HOURLY, stay));

        if (stay.elapsedDuration().compareTo(MAX_SPECIAL_STAY) <= 0) {
            candidates.addAll(evaluate(earlyBirdPolicy, PricingPolicyType.EARLY_BIRD, stay));
            candidates.addAll(evaluate(nightOwlPolicy, PricingPolicyType.NIGHT_OWL, stay));
        }

        FareCandidate winner = candidates.stream()
                .min(Comparator.comparing(FareCandidate::exactAmount)
                        .thenComparingInt(candidate -> candidate.policy().tieBreakPriority()))
                .orElseThrow(InvalidPricingConfigurationException::new);
        return FareQuote.from(winner);
    }

    private List<FareCandidate> evaluate(
            RatePolicy policy, PricingPolicyType expectedType, ParkingStay stay) {
        Optional<FareCandidate> result = Objects.requireNonNull(
                policy.evaluate(stay, context), "Rate policy result is required");
        if (result.isEmpty()) {
            if (expectedType == PricingPolicyType.STANDARD_HOURLY) {
                throw new InvalidPricingConfigurationException();
            }
            return List.of();
        }

        FareCandidate candidate = result.orElseThrow();
        if (candidate.policy() != expectedType) {
            throw new InvalidPricingConfigurationException();
        }
        return List.of(candidate);
    }
}
