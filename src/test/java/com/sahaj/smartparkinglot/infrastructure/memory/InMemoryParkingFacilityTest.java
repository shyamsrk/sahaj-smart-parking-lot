package com.sahaj.smartparkinglot.infrastructure.memory;

import com.sahaj.smartparkinglot.application.port.TicketIdGenerator;
import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.FareQuote;
import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.Money;
import com.sahaj.smartparkinglot.domain.ParkingSpot;
import com.sahaj.smartparkinglot.domain.ParkingTicket;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.SpotType;
import com.sahaj.smartparkinglot.domain.TicketStatus;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.exception.DuplicateTicketIdException;
import com.sahaj.smartparkinglot.domain.exception.InvalidParkingStayException;
import com.sahaj.smartparkinglot.domain.exception.NoCompatibleSpotAvailableException;
import com.sahaj.smartparkinglot.domain.exception.TicketAlreadyExitedException;
import com.sahaj.smartparkinglot.domain.exception.TicketNotFoundException;
import com.sahaj.smartparkinglot.domain.pricing.FareCalculator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryParkingFacilityTest {
    private static final Instant ENTRY_AT = Instant.parse("2026-01-05T16:00:00Z");
    private static final Instant EXIT_AT = Instant.parse("2026-01-05T17:00:00Z");
    private static final UUID FIXED_ID = UUID.fromString("6d10d96b-8173-4aeb-bfaa-5c53ee32464e");

    @Test
    void entryIssuesTicketWithAssignedSpotAndDefaultLoyaltyTier() {
        InMemoryParkingFacility facility = facility(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)),
                () -> FIXED_ID,
                stay -> quote("STANDARD_HOURLY", "5.00"));

        ParkingTicket ticket = facility.enter(VehicleType.CAR, ENTRY_AT);

        assertEquals(FIXED_ID, ticket.id());
        assertEquals("C-01", ticket.spotId());
        assertEquals(LoyaltyTier.NONE, ticket.loyaltyTier());
        assertEquals(TicketStatus.ACTIVE, ticket.status());
    }

    @Test
    void noCompatibleSpotMeansNoTicketIsRegistered() {
        InMemoryTicketRegistry registry = new InMemoryTicketRegistry();
        InMemoryParkingFacility facility = new InMemoryParkingFacility(
                new InMemorySpotInventory(List.of(new ParkingSpot("L-01", SpotType.LARGE))),
                registry,
                () -> FIXED_ID,
                stay -> quote("STANDARD_HOURLY", "5.00"));

        assertThrows(NoCompatibleSpotAvailableException.class,
                () -> facility.enter(VehicleType.CAR, ENTRY_AT));
        assertTrue(registry.findById(FIXED_ID).isEmpty());
    }

    @Test
    void failedTicketRegistrationReleasesTheReservedSpot() {
        InMemorySpotInventory inventory = new InMemorySpotInventory(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)));
        InMemoryTicketRegistry registry = new InMemoryTicketRegistry();
        registry.register(new ParkingTicket(FIXED_ID, ENTRY_AT, VehicleType.CAR, "C-01"));
        InMemoryParkingFacility facility = new InMemoryParkingFacility(
                inventory, registry, () -> FIXED_ID, stay -> quote("STANDARD_HOURLY", "5.00"));

        assertThrows(DuplicateTicketIdException.class,
                () -> facility.enter(VehicleType.CAR, ENTRY_AT));
        ParkingSpot available = inventory.reserveCompatibleSpot(VehicleType.CAR, UUID.randomUUID());
        assertEquals("C-01", available.id());
    }

    @Test
    void successfulExitQuotesMarksTicketAndReleasesSpotOnce() {
        AtomicInteger quoteCalls = new AtomicInteger();
        InMemorySpotInventory inventory = new InMemorySpotInventory(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)));
        InMemoryTicketRegistry registry = new InMemoryTicketRegistry();
        InMemoryParkingFacility facility = new InMemoryParkingFacility(
                inventory,
                registry,
                () -> FIXED_ID,
                stay -> {
                    quoteCalls.incrementAndGet();
                    return quote("STANDARD_HOURLY", "5.00");
                });
        ParkingTicket ticket = facility.enter(VehicleType.CAR, ENTRY_AT);

        FareQuote quote = facility.exit(ticket.id(), EXIT_AT);

        assertEquals(Money.of("5.00"), quote.amount());
        assertEquals(TicketStatus.EXITED, ticket.status());
        assertEquals("C-01", inventory.reserveCompatibleSpot(
                VehicleType.CAR, UUID.randomUUID()).id());
        assertThrows(TicketAlreadyExitedException.class,
                () -> facility.exit(ticket.id(), EXIT_AT.plusSeconds(60)));
        assertEquals(1, quoteCalls.get());
    }

    @Test
    void invalidExitOrPricingFailureLeavesTicketAndSpotActive() {
        InMemorySpotInventory inventory = new InMemorySpotInventory(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)));
        InMemoryParkingFacility facility = new InMemoryParkingFacility(
                inventory,
                new InMemoryTicketRegistry(),
                () -> FIXED_ID,
                stay -> {
                    throw new IllegalStateException();
                });
        ParkingTicket ticket = facility.enter(VehicleType.CAR, ENTRY_AT);

        assertThrows(InvalidParkingStayException.class,
                () -> facility.exit(ticket.id(), ENTRY_AT));
        assertEquals(TicketStatus.ACTIVE, ticket.status());
        assertThrows(IllegalStateException.class, () -> facility.exit(ticket.id(), EXIT_AT));
        assertEquals(TicketStatus.ACTIVE, ticket.status());
        assertThrows(NoCompatibleSpotAvailableException.class,
                () -> inventory.reserveCompatibleSpot(VehicleType.CAR, UUID.randomUUID()));
    }

    @Test
    void rejectsUnknownTicket() {
        InMemoryParkingFacility facility = facility(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)),
                () -> FIXED_ID,
                stay -> quote("STANDARD_HOURLY", "5.00"));

        assertThrows(TicketNotFoundException.class,
                () -> facility.exit(UUID.randomUUID(), EXIT_AT));
    }

    @Test
    void concurrentEntryRequestsCannotReserveTheSameSpot() throws Exception {
        InMemoryParkingFacility facility = facility(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)),
                incrementingIds(),
                stay -> quote("STANDARD_HOURLY", "5.00"));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> tryEntry(facility, start));
            Future<Boolean> second = executor.submit(() -> tryEntry(facility, start));
            start.countDown();

            assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentExitRequestsOnlyQuoteAndExitOnce() throws Exception {
        AtomicInteger quoteCalls = new AtomicInteger();
        InMemoryParkingFacility facility = facility(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)),
                () -> FIXED_ID,
                stay -> {
                    quoteCalls.incrementAndGet();
                    return quote("STANDARD_HOURLY", "5.00");
                });
        ParkingTicket ticket = facility.enter(VehicleType.CAR, ENTRY_AT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> tryExit(facility, ticket.id(), start));
            Future<Boolean> second = executor.submit(() -> tryExit(facility, ticket.id(), start));
            start.countDown();

            assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
            assertEquals(1, quoteCalls.get());
            assertEquals(TicketStatus.EXITED, ticket.status());
        } finally {
            executor.shutdownNow();
        }
    }

    private static InMemoryParkingFacility facility(
            List<ParkingSpot> spots, TicketIdGenerator ids, FareCalculator calculator) {
        return new InMemoryParkingFacility(
                new InMemorySpotInventory(spots),
                new InMemoryTicketRegistry(),
                ids,
                calculator);
    }

    private static FareQuote quote(String policy, String amount) {
        return FareQuote.from(new FareCandidate(
                PricingPolicyType.valueOf(policy), Money.of(amount)));
    }

    private static TicketIdGenerator incrementingIds() {
        AtomicLong next = new AtomicLong();
        return () -> new UUID(0, next.incrementAndGet());
    }

    private static boolean tryEntry(InMemoryParkingFacility facility, CountDownLatch start)
            throws InterruptedException {
        start.await();
        try {
            facility.enter(VehicleType.CAR, ENTRY_AT);
            return true;
        } catch (NoCompatibleSpotAvailableException expected) {
            return false;
        }
    }

    private static boolean tryExit(
            InMemoryParkingFacility facility, UUID ticketId, CountDownLatch start)
            throws InterruptedException {
        start.await();
        try {
            facility.exit(ticketId, EXIT_AT);
            return true;
        } catch (TicketAlreadyExitedException expected) {
            return false;
        }
    }
}
