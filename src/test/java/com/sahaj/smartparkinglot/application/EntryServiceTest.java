package com.sahaj.smartparkinglot.application;

import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.ParkingTicket;
import com.sahaj.smartparkinglot.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EntryServiceTest {
    @Test
    void defaultsOmittedLoyaltyTierToNone() {
        Instant entryAt = Instant.parse("2026-01-05T16:00:00Z");
        ParkingTicket expected = new ParkingTicket(
                UUID.fromString("6d10d96b-8173-4aeb-bfaa-5c53ee32464e"),
                entryAt,
                VehicleType.CAR,
                "C-01");
        RecordingEntryService service = new RecordingEntryService(expected);

        ParkingTicket actual = service.enter(VehicleType.CAR, entryAt);

        assertSame(expected, actual);
        assertEquals(VehicleType.CAR, service.vehicleType);
        assertEquals(entryAt, service.entryAt);
        assertEquals(LoyaltyTier.NONE, service.loyaltyTier);
    }

    private static final class RecordingEntryService implements EntryService {
        private final ParkingTicket ticket;
        private VehicleType vehicleType;
        private Instant entryAt;
        private LoyaltyTier loyaltyTier;

        private RecordingEntryService(ParkingTicket ticket) {
            this.ticket = ticket;
        }

        @Override
        public ParkingTicket enter(VehicleType vehicleType, Instant entryAt, LoyaltyTier loyaltyTier) {
            this.vehicleType = vehicleType;
            this.entryAt = entryAt;
            this.loyaltyTier = loyaltyTier;
            return ticket;
        }
    }
}
