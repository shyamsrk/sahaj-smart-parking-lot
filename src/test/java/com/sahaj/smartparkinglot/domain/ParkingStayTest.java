package com.sahaj.smartparkinglot.domain;

import com.sahaj.smartparkinglot.domain.exception.InvalidParkingStayException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingStayTest {
    @Test
    void createsStayFromActiveTicketAndCalculatesElapsedInstantDuration() {
        Instant entry = Instant.parse("2026-03-08T09:30:00Z");
        ParkingTicket ticket = new ParkingTicket(
                UUID.randomUUID(), entry, VehicleType.CAR, "C-01", LoyaltyTier.NONE);

        ParkingStay stay = new ParkingStay(ticket, Instant.parse("2026-03-08T10:30:00Z"));

        assertEquals(Duration.ofHours(1), stay.elapsedDuration());
        assertEquals(entry, stay.entryAt());
    }

    @Test
    void rejectsExitAtOrBeforeEntry() {
        Instant entry = Instant.parse("2026-01-05T16:00:00Z");
        assertThrows(InvalidParkingStayException.class,
                () -> new ParkingStay(UUID.randomUUID(), "C-01", VehicleType.CAR,
                        LoyaltyTier.NONE, entry, entry));
        assertThrows(InvalidParkingStayException.class,
                () -> new ParkingStay(UUID.randomUUID(), "C-01", VehicleType.CAR,
                        LoyaltyTier.NONE, entry, entry.minusSeconds(1)));
    }

    @Test
    void rejectsCreatingStayForExitedTicket() {
        ParkingTicket ticket = new ParkingTicket(
                UUID.randomUUID(), Instant.parse("2026-01-05T16:00:00Z"), VehicleType.CAR, "C-01");
        ticket.markExited();

        assertThrows(InvalidParkingStayException.class,
                () -> new ParkingStay(ticket, Instant.parse("2026-01-05T17:00:00Z")));
    }

    @Test
    void rejectsMissingTicketOrExitInstant() {
        Instant entry = Instant.parse("2026-01-05T16:00:00Z");

        assertThrows(NullPointerException.class,
                () -> new ParkingStay((ParkingTicket) null, entry.plusSeconds(60)));
        assertThrows(NullPointerException.class,
                () -> new ParkingStay(UUID.randomUUID(), "C-01", VehicleType.CAR,
                        LoyaltyTier.NONE, entry, null));
    }

    @Test
    void rejectsMissingOrBlankSpotId() {
        Instant entry = Instant.parse("2026-01-05T16:00:00Z");
        Instant exit = entry.plusSeconds(60);

        assertThrows(IllegalArgumentException.class,
                () -> new ParkingStay(UUID.randomUUID(), null, VehicleType.CAR,
                        LoyaltyTier.NONE, entry, exit));
        assertThrows(IllegalArgumentException.class,
                () -> new ParkingStay(UUID.randomUUID(), " ", VehicleType.CAR,
                        LoyaltyTier.NONE, entry, exit));
    }
}
