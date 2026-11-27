package com.sahaj.smartparkinglot.domain;

import java.math.BigDecimal;

public enum VehicleType {
    MOTORCYCLE("0.8", SpotType.COMPACT),
    CAR("1.0", SpotType.COMPACT),
    BUS("2.0", SpotType.LARGE);

    private final BigDecimal rateMultiplier;
    private final SpotType compatibleSpotType;

    VehicleType(String rateMultiplier, SpotType compatibleSpotType) {
        this.rateMultiplier = new BigDecimal(rateMultiplier);
        this.compatibleSpotType = compatibleSpotType;
    }

    public BigDecimal rateMultiplier() {
        return rateMultiplier;
    }

    public SpotType compatibleSpotType() {
        return compatibleSpotType;
    }
}
