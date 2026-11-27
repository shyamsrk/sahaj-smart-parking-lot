package com.sahaj.smartparkinglot.application.port;

import com.sahaj.smartparkinglot.domain.ParkingSpot;
import com.sahaj.smartparkinglot.domain.VehicleType;

import java.util.UUID;

public interface SpotInventory {
    ParkingSpot reserveCompatibleSpot(VehicleType vehicleType, UUID ticketId);

    void releaseSpot(String spotId, UUID ticketId);
}
