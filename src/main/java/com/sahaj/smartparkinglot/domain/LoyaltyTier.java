package com.sahaj.smartparkinglot.domain;

import java.math.BigDecimal;

public enum LoyaltyTier {
    NONE("0.00"),
    SILVER("0.10"),
    GOLD("0.20"),
    PLATINUM("0.30");

    private final BigDecimal discountRate;

    LoyaltyTier(String discountRate) {
        this.discountRate = new BigDecimal(discountRate);
    }

    public BigDecimal discountRate() {
        return discountRate;
    }
}
