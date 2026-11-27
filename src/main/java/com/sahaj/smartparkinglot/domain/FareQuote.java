package com.sahaj.smartparkinglot.domain;

import java.util.Objects;

public record FareQuote(PricingPolicyType policy, Money exactAmount, Money amount) {
    public FareQuote {
        Objects.requireNonNull(policy, "Pricing policy is required");
        Objects.requireNonNull(exactAmount, "Exact fare is required");
        Objects.requireNonNull(amount, "Final fare is required");
        if (!exactAmount.roundToCents().equals(amount)) {
            throw new IllegalArgumentException("Final fare must be the exact fare rounded to cents");
        }
    }

    public static FareQuote from(FareCandidate candidate) {
        Objects.requireNonNull(candidate, "Fare candidate is required");
        return new FareQuote(
                candidate.policy(),
                candidate.exactAmount(),
                candidate.exactAmount().roundToCents());
    }
}
