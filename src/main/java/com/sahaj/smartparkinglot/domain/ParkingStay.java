package com.sahaj.smartparkinglot.domain;

import com.sahaj.smartparkinglot.domain.exception.InvalidParkingStayException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ParkingStay(
        UUID ticketId,
        String spotId,
        VehicleType vehicleType,
        LoyaltyTier loyaltyTier,
        Instant entryAt,
        Instant exitAt) {

    public ParkingStay {
        Objects.requireNonNull(ticketId, "Ticket ID is required");
        if (spotId == null || spotId.isBlank()) {
            throw new IllegalArgumentException("Spot ID must not be blank");
        }
        Objects.requireNonNull(vehicleType, "Vehicle type is required");
        Objects.requireNonNull(loyaltyTier, "Loyalty tier is required");
        Objects.requireNonNull(entryAt, "Entry instant is required");
        Objects.requireNonNull(exitAt, "Exit instant is required");
        if (!exitAt.isAfter(entryAt)) {
            throw new InvalidParkingStayException();
        }
    }

    public ParkingStay(ParkingTicket ticket, Instant exitAt) {
        this(
                Objects.requireNonNull(ticket, "Ticket is required").id(),
                ticket.spotId(),
                ticket.vehicleType(),
                ticket.loyaltyTier(),
                ticket.entryAt(),
                exitAt);
        if (!ticket.isActive()) {
            throw new InvalidParkingStayException();
        }
    }

    public Duration elapsedDuration() {
        return Duration.between(entryAt, exitAt);
    }
}
