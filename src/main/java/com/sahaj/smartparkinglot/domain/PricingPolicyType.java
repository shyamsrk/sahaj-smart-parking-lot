package com.sahaj.smartparkinglot.domain;

public enum PricingPolicyType {
    EARLY_BIRD(0),
    NIGHT_OWL(1),
    STANDARD_HOURLY(2);

    private final int tieBreakPriority;

    PricingPolicyType(int tieBreakPriority) {
        this.tieBreakPriority = tieBreakPriority;
    }

    public int tieBreakPriority() {
        return tieBreakPriority;
    }
}
