package com.sahaj.smartparkinglot.infrastructure.memory;

import com.sahaj.smartparkinglot.application.port.TicketRegistry;
import com.sahaj.smartparkinglot.domain.ParkingTicket;
import com.sahaj.smartparkinglot.domain.exception.DuplicateTicketIdException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryTicketRegistry implements TicketRegistry {
    private final Map<UUID, ParkingTicket> tickets = new HashMap<>();

    @Override
    public synchronized Optional<ParkingTicket> findById(UUID ticketId) {
        Objects.requireNonNull(ticketId, "Ticket ID is required");
        return Optional.ofNullable(tickets.get(ticketId));
    }

    @Override
    public synchronized List<ParkingTicket> findAll() {
        return List.copyOf(tickets.values());
    }

    @Override
    public synchronized void register(ParkingTicket ticket) {
        Objects.requireNonNull(ticket, "Ticket is required");
        if (tickets.putIfAbsent(ticket.id(), ticket) != null) {
            throw new DuplicateTicketIdException();
        }
    }
}
