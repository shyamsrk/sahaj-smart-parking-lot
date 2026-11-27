package com.sahaj.smartparkinglot.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoyaltyTierTest {
    @Test
    void providesConfiguredDiscountRates() {
        assertEquals(new BigDecimal("0.00"), LoyaltyTier.NONE.discountRate());
        assertEquals(new BigDecimal("0.10"), LoyaltyTier.SILVER.discountRate());
        assertEquals(new BigDecimal("0.20"), LoyaltyTier.GOLD.discountRate());
        assertEquals(new BigDecimal("0.30"), LoyaltyTier.PLATINUM.discountRate());
    }
}
