package com.sahaj.smartparkinglot.infrastructure.memory;

import com.sahaj.smartparkinglot.application.EntryService;
import com.sahaj.smartparkinglot.application.ExitService;
import com.sahaj.smartparkinglot.application.port.SpotInventory;
import com.sahaj.smartparkinglot.application.port.TicketIdGenerator;
import com.sahaj.smartparkinglot.application.port.TicketRegistry;
import com.sahaj.smartparkinglot.domain.FareQuote;
import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.ParkingSpot;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.ParkingTicket;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.exception.DuplicateTicketIdException;
import com.sahaj.smartparkinglot.domain.exception.TicketAlreadyExitedException;
import com.sahaj.smartparkinglot.domain.exception.TicketNotFoundException;
import com.sahaj.smartparkinglot.domain.pricing.FareCalculator;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class InMemoryParkingFacility implements EntryService, ExitService {
    private final SpotInventory spotInventory;
    private final TicketRegistry ticketRegistry;
    private final TicketIdGenerator ticketIdGenerator;
    private final FareCalculator fareCalculator;

    public InMemoryParkingFacility(
            SpotInventory spotInventory,
            TicketRegistry ticketRegistry,
            TicketIdGenerator ticketIdGenerator,
            FareCalculator fareCalculator) {
        this.spotInventory = Objects.requireNonNull(spotInventory, "Spot inventory is required");
        this.ticketRegistry = Objects.requireNonNull(ticketRegistry, "Ticket registry is required");
        this.ticketIdGenerator = Objects.requireNonNull(ticketIdGenerator, "Ticket ID generator is required");
        this.fareCalculator = Objects.requireNonNull(fareCalculator, "Fare calculator is required");
    }

    @Override
    public synchronized ParkingTicket enter(
            VehicleType vehicleType, Instant entryAt, LoyaltyTier loyaltyTier) {
        Objects.requireNonNull(vehicleType, "Vehicle type is required");
        Objects.requireNonNull(entryAt, "Entry instant is required");
        Objects.requireNonNull(loyaltyTier, "Loyalty tier is required");
        UUID ticketId = Objects.requireNonNull(ticketIdGenerator.nextId(), "Generated ticket ID is required");
        ParkingSpot spot = Objects.requireNonNull(
                spotInventory.reserveCompatibleSpot(vehicleType, ticketId),
                "Reserved spot is required");

        try {
            ParkingTicket ticket = new ParkingTicket(
                    ticketId, entryAt, vehicleType, spot.id(), loyaltyTier);
            ticketRegistry.register(ticket);
            return ticket;
        } catch (IllegalArgumentException | NullPointerException | DuplicateTicketIdException failure) {
            spotInventory.releaseSpot(spot.id(), ticketId);
            throw failure;
        }
    }

    @Override
    public synchronized FareQuote exit(UUID ticketId, Instant exitAt) {
        Objects.requireNonNull(ticketId, "Ticket ID is required");
        Objects.requireNonNull(exitAt, "Exit instant is required");
        ParkingTicket ticket = Objects.requireNonNull(
                        ticketRegistry.findById(ticketId), "Ticket lookup result is required")
                .orElseThrow(TicketNotFoundException::new);
        if (!ticket.isActive()) {
            throw new TicketAlreadyExitedException();
        }

        ParkingStay stay = new ParkingStay(ticket, exitAt);
        FareQuote quote = Objects.requireNonNull(
                fareCalculator.calculate(stay), "Fare quote is required");
        ticket.markExited();
        spotInventory.releaseSpot(ticket.spotId(), ticket.id());
        return quote;
    }

    public synchronized List<ParkingTicket> activeTickets() {
        return ticketRegistry.findAll().stream()
                .filter(ParkingTicket::isActive)
                .sorted(Comparator.comparing(ParkingTicket::spotId))
                .toList();
    }
}
