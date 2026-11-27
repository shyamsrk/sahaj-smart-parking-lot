package com.sahaj.smartparkinglot.domain;

import com.sahaj.smartparkinglot.domain.exception.IncompatibleVehicleException;
import com.sahaj.smartparkinglot.domain.exception.SpotAlreadyOccupiedException;
import com.sahaj.smartparkinglot.domain.exception.SpotNotOccupiedByTicketException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ParkingSpot implements Comparable<ParkingSpot> {
    private final String id;
    private final SpotType type;
    private UUID occupyingTicketId;

    public ParkingSpot(String id, SpotType type) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Spot ID must not be blank");
        }
        this.id = id;
        this.type = Objects.requireNonNull(type, "Spot type is required");
    }

    public String id() {
        return id;
    }

    public SpotType type() {
        return type;
    }

    public synchronized boolean isOccupied() {
        return occupyingTicketId != null;
    }

    public synchronized Optional<UUID> occupyingTicketId() {
        return Optional.ofNullable(occupyingTicketId);
    }

    public boolean isCompatibleWith(VehicleType vehicleType) {
        Objects.requireNonNull(vehicleType, "Vehicle type is required");
        return type == vehicleType.compatibleSpotType();
    }

    public synchronized void reserve(UUID ticketId, VehicleType vehicleType) {
        Objects.requireNonNull(ticketId, "Ticket ID is required");
        Objects.requireNonNull(vehicleType, "Vehicle type is required");
        if (!isCompatibleWith(vehicleType)) {
            throw new IncompatibleVehicleException();
        }
        if (occupyingTicketId != null) {
            throw new SpotAlreadyOccupiedException();
        }
        occupyingTicketId = ticketId;
    }

    public synchronized void release(UUID ticketId) {
        Objects.requireNonNull(ticketId, "Ticket ID is required");
        if (!ticketId.equals(occupyingTicketId)) {
            throw new SpotNotOccupiedByTicketException();
        }
        occupyingTicketId = null;
    }

    @Override
    public int compareTo(ParkingSpot other) {
        Objects.requireNonNull(other, "Spot is required");
        return id.compareTo(other.id);
    }
}
