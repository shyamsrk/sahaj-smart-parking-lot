package com.sahaj.smartparkinglot.application;

import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.FareQuote;
import com.sahaj.smartparkinglot.domain.Money;
import com.sahaj.smartparkinglot.domain.ParkingSpot;
import com.sahaj.smartparkinglot.domain.ParkingTicket;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.SpotType;
import com.sahaj.smartparkinglot.domain.TicketStatus;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemoryParkingFacility;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemorySpotInventory;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemoryTicketRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExitServiceTest {
    @Test
    void exitsTicketThroughServiceContractAndReleasesItsSpot() {
        UUID ticketId = UUID.fromString("6d10d96b-8173-4aeb-bfaa-5c53ee32464e");
        Instant entryAt = Instant.parse("2026-01-05T16:00:00Z");
        Instant exitAt = Instant.parse("2026-01-05T17:00:00Z");
        InMemorySpotInventory inventory = new InMemorySpotInventory(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)));
        InMemoryParkingFacility facility = new InMemoryParkingFacility(
                inventory,
                new InMemoryTicketRegistry(),
                () -> ticketId,
                stay -> FareQuote.from(new FareCandidate(
                        PricingPolicyType.STANDARD_HOURLY, Money.of("5.00"))));
        ParkingTicket ticket = facility.enter(VehicleType.CAR, entryAt);
        ExitService exitService = facility;

        FareQuote quote = exitService.exit(ticket.id(), exitAt);

        assertEquals(Money.of("5.00"), quote.amount());
        assertEquals(TicketStatus.EXITED, ticket.status());
        assertEquals("C-01", inventory.reserveCompatibleSpot(
                VehicleType.CAR, UUID.randomUUID()).id());
    }
}
