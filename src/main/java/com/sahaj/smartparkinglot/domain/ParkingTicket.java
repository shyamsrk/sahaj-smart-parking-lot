package com.sahaj.smartparkinglot.domain;

import com.sahaj.smartparkinglot.domain.exception.TicketAlreadyExitedException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ParkingTicket {
    private final UUID id;
    private final Instant entryAt;
    private final VehicleType vehicleType;
    private final String spotId;
    private final LoyaltyTier loyaltyTier;
    private TicketStatus status = TicketStatus.ACTIVE;

    public ParkingTicket(UUID id, Instant entryAt, VehicleType vehicleType, String spotId) {
        this(id, entryAt, vehicleType, spotId, LoyaltyTier.NONE);
    }

    public ParkingTicket(
            UUID id,
            Instant entryAt,
            VehicleType vehicleType,
            String spotId,
            LoyaltyTier loyaltyTier) {
        this.id = Objects.requireNonNull(id, "Ticket ID is required");
        this.entryAt = Objects.requireNonNull(entryAt, "Entry instant is required");
        this.vehicleType = Objects.requireNonNull(vehicleType, "Vehicle type is required");
        if (spotId == null || spotId.isBlank()) {
            throw new IllegalArgumentException("Spot ID must not be blank");
        }
        this.spotId = spotId;
        this.loyaltyTier = Objects.requireNonNull(loyaltyTier, "Loyalty tier is required");
    }

    public UUID id() {
        return id;
    }

    public Instant entryAt() {
        return entryAt;
    }

    public VehicleType vehicleType() {
        return vehicleType;
    }

    public String spotId() {
        return spotId;
    }

    public LoyaltyTier loyaltyTier() {
        return loyaltyTier;
    }

    public synchronized TicketStatus status() {
        return status;
    }

    public synchronized boolean isActive() {
        return status == TicketStatus.ACTIVE;
    }

    public synchronized void markExited() {
        if (status == TicketStatus.EXITED) {
            throw new TicketAlreadyExitedException();
        }
        status = TicketStatus.EXITED;
    }
}
