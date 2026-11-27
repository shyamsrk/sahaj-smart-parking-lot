package com.sahaj.smartparkinglot.domain;

import com.sahaj.smartparkinglot.domain.exception.TicketAlreadyExitedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingTicketTest {
    private static final UUID ID = UUID.fromString("6d10d96b-8173-4aeb-bfaa-5c53ee32464e");
    private static final Instant ENTRY_AT = Instant.parse("2026-01-05T16:00:00Z");

    @Test
    void defaultsLoyaltyToNoneAndStartsActive() {
        ParkingTicket ticket = new ParkingTicket(ID, ENTRY_AT, VehicleType.CAR, "C-01");

        assertEquals(LoyaltyTier.NONE, ticket.loyaltyTier());
        assertEquals(TicketStatus.ACTIVE, ticket.status());
        assertTrue(ticket.isActive());
    }

    @Test
    void retainsProvidedLoyaltyTierAndPermitsOnlyOneExit() {
        ParkingTicket ticket = new ParkingTicket(ID, ENTRY_AT, VehicleType.CAR, "C-01", LoyaltyTier.GOLD);

        assertEquals(LoyaltyTier.GOLD, ticket.loyaltyTier());
        ticket.markExited();
        assertEquals(TicketStatus.EXITED, ticket.status());
        assertFalse(ticket.isActive());
        assertThrows(TicketAlreadyExitedException.class, ticket::markExited);
    }
}
