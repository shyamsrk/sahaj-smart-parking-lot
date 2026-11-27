package com.sahaj.smartparkinglot.infrastructure.memory;

import com.sahaj.smartparkinglot.application.port.SpotInventory;
import com.sahaj.smartparkinglot.domain.ParkingSpot;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.exception.InvalidSpotException;
import com.sahaj.smartparkinglot.domain.exception.NoCompatibleSpotAvailableException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class InMemorySpotInventory implements SpotInventory {
    private final List<ParkingSpot> spots;

    public InMemorySpotInventory(Collection<ParkingSpot> spots) {
        Objects.requireNonNull(spots, "Spots are required");
        Set<String> ids = new HashSet<>();
        List<ParkingSpot> sortedSpots = new ArrayList<>(spots.size());
        for (ParkingSpot spot : spots) {
            Objects.requireNonNull(spot, "Spot is required");
            if (spot.isOccupied()) {
                throw new IllegalArgumentException("Configured spots must start unoccupied");
            }
            if (!ids.add(spot.id())) {
                throw new IllegalArgumentException("Spot IDs must be unique");
            }
            sortedSpots.add(new ParkingSpot(spot.id(), spot.type()));
        }
        sortedSpots.sort(ParkingSpot::compareTo);
        this.spots = List.copyOf(sortedSpots);
    }

    @Override
    public synchronized ParkingSpot reserveCompatibleSpot(VehicleType vehicleType, UUID ticketId) {
        Objects.requireNonNull(vehicleType, "Vehicle type is required");
        Objects.requireNonNull(ticketId, "Ticket ID is required");
        for (ParkingSpot spot : spots) {
            if (!spot.isOccupied() && spot.isCompatibleWith(vehicleType)) {
                spot.reserve(ticketId, vehicleType);
                return spot;
            }
        }
        throw new NoCompatibleSpotAvailableException();
    }

    @Override
    public synchronized void releaseSpot(String spotId, UUID ticketId) {
        Objects.requireNonNull(spotId, "Spot ID is required");
        ParkingSpot spot = spots.stream()
                .filter(candidate -> candidate.id().equals(spotId))
                .findFirst()
                .orElseThrow(InvalidSpotException::new);
        spot.release(ticketId);
    }
}
