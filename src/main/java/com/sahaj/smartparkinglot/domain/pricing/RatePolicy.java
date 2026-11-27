package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.time.PricingContext;

import java.util.Optional;

@FunctionalInterface
public interface RatePolicy {
    Optional<FareCandidate> evaluate(ParkingStay stay, PricingContext context);
}
