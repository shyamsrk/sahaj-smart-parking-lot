package com.sahaj.smartparkinglot.domain;

import java.util.Objects;

public record FareCandidate(PricingPolicyType policy, Money exactAmount) {
    public FareCandidate {
        Objects.requireNonNull(policy, "Pricing policy is required");
        Objects.requireNonNull(exactAmount, "Exact fare is required");
    }
}
