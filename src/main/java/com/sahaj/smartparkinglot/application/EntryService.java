package com.sahaj.smartparkinglot.application;

import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.ParkingTicket;
import com.sahaj.smartparkinglot.domain.VehicleType;

import java.time.Instant;

public interface EntryService {
    ParkingTicket enter(VehicleType vehicleType, Instant entryAt, LoyaltyTier loyaltyTier);

    default ParkingTicket enter(VehicleType vehicleType, Instant entryAt) {
        return enter(vehicleType, entryAt, LoyaltyTier.NONE);
    }
}
