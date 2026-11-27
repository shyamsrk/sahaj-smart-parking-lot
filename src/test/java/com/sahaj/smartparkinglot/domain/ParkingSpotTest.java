package com.sahaj.smartparkinglot.domain;

import com.sahaj.smartparkinglot.domain.exception.IncompatibleVehicleException;
import com.sahaj.smartparkinglot.domain.exception.SpotAlreadyOccupiedException;
import com.sahaj.smartparkinglot.domain.exception.SpotNotOccupiedByTicketException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingSpotTest {
    @Test
    void reservesCompatibleSpotAndReleasesItForTheSameTicket() {
        UUID ticketId = UUID.randomUUID();
        ParkingSpot spot = new ParkingSpot("C-01", SpotType.COMPACT);

        assertTrue(spot.isCompatibleWith(VehicleType.MOTORCYCLE));
        spot.reserve(ticketId, VehicleType.CAR);

        assertTrue(spot.isOccupied());
        assertEquals(ticketId, spot.occupyingTicketId().orElseThrow());
        spot.release(ticketId);
        assertFalse(spot.isOccupied());
    }

    @Test
    void rejectsIncompatibleVehicleAndAlreadyOccupiedSpot() {
        ParkingSpot largeSpot = new ParkingSpot("L-01", SpotType.LARGE);
        assertThrows(IncompatibleVehicleException.class,
                () -> largeSpot.reserve(UUID.randomUUID(), VehicleType.CAR));

        UUID ticketId = UUID.randomUUID();
        ParkingSpot compactSpot = new ParkingSpot("C-01", SpotType.COMPACT);
        compactSpot.reserve(ticketId, VehicleType.CAR);
        assertThrows(SpotAlreadyOccupiedException.class,
                () -> compactSpot.reserve(UUID.randomUUID(), VehicleType.CAR));
    }

    @Test
    void onlyOccupyingTicketCanReleaseSpot() {
        ParkingSpot spot = new ParkingSpot("C-01", SpotType.COMPACT);
        spot.reserve(UUID.randomUUID(), VehicleType.CAR);

        assertThrows(SpotNotOccupiedByTicketException.class,
                () -> spot.release(UUID.randomUUID()));
        assertTrue(spot.isOccupied());
    }

    @Test
    void ordersSpotsByNaturalIdOrder() {
        ParkingSpot first = new ParkingSpot("A-02", SpotType.COMPACT);
        ParkingSpot second = new ParkingSpot("B-01", SpotType.COMPACT);

        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    void rejectsBlankSpotId() {
        assertThrows(IllegalArgumentException.class, () -> new ParkingSpot(" ", SpotType.COMPACT));
        assertThrows(IllegalArgumentException.class, () -> new ParkingSpot(null, SpotType.COMPACT));
    }
}
