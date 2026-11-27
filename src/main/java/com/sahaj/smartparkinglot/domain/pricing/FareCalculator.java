package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.FareQuote;
import com.sahaj.smartparkinglot.domain.ParkingStay;

@FunctionalInterface
public interface FareCalculator {
    FareQuote calculate(ParkingStay stay);
}
