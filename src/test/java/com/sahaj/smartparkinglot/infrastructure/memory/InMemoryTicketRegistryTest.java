package com.sahaj.smartparkinglot.infrastructure.memory;

import com.sahaj.smartparkinglot.domain.ParkingTicket;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.exception.DuplicateTicketIdException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTicketRegistryTest {
    @Test
    void findsRegisteredTicketAndReturnsEmptyForUnknownId() {
        InMemoryTicketRegistry registry = new InMemoryTicketRegistry();
        UUID ticketId = UUID.randomUUID();
        ParkingTicket ticket = new ParkingTicket(
                ticketId, Instant.parse("2026-01-05T16:00:00Z"), VehicleType.CAR, "C-01");
        registry.register(ticket);

        assertSame(ticket, registry.findById(ticketId).orElseThrow());
        assertEquals(List.of(ticket), registry.findAll());
        assertTrue(registry.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void rejectsDuplicateTicketIdsWithoutReplacingTheOriginal() {
        InMemoryTicketRegistry registry = new InMemoryTicketRegistry();
        UUID ticketId = UUID.randomUUID();
        ParkingTicket original = new ParkingTicket(
                ticketId, Instant.parse("2026-01-05T16:00:00Z"), VehicleType.CAR, "C-01");
        registry.register(original);

        assertThrows(DuplicateTicketIdException.class, () -> registry.register(
                new ParkingTicket(ticketId, Instant.parse("2026-01-05T17:00:00Z"),
                        VehicleType.CAR, "C-02")));
        assertSame(original, registry.findById(ticketId).orElseThrow());
    }
}
