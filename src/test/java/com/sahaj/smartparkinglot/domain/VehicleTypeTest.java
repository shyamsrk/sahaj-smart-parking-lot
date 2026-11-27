package com.sahaj.smartparkinglot.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleTypeTest {
    @Test
    void providesConfiguredMultiplierAndCompatibleSpotType() {
        assertEquals(new java.math.BigDecimal("0.8"), VehicleType.MOTORCYCLE.rateMultiplier());
        assertEquals(SpotType.COMPACT, VehicleType.MOTORCYCLE.compatibleSpotType());
        assertEquals(new java.math.BigDecimal("1.0"), VehicleType.CAR.rateMultiplier());
        assertEquals(SpotType.COMPACT, VehicleType.CAR.compatibleSpotType());
        assertEquals(new java.math.BigDecimal("2.0"), VehicleType.BUS.rateMultiplier());
        assertEquals(SpotType.LARGE, VehicleType.BUS.compatibleSpotType());
    }
}
