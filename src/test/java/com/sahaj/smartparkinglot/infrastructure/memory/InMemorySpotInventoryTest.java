package com.sahaj.smartparkinglot.infrastructure.memory;

import com.sahaj.smartparkinglot.domain.ParkingSpot;
import com.sahaj.smartparkinglot.domain.SpotType;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.exception.InvalidSpotException;
import com.sahaj.smartparkinglot.domain.exception.NoCompatibleSpotAvailableException;
import com.sahaj.smartparkinglot.domain.exception.SpotNotOccupiedByTicketException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemorySpotInventoryTest {
    @Test
    void assignsAvailableCompatibleSpotsInNaturalIdOrder() {
        InMemorySpotInventory inventory = new InMemorySpotInventory(List.of(
                new ParkingSpot("C-10", SpotType.COMPACT),
                new ParkingSpot("C-02", SpotType.COMPACT),
                new ParkingSpot("C-01", SpotType.COMPACT)));

        assertEquals("C-01", inventory.reserveCompatibleSpot(
                VehicleType.CAR, UUID.randomUUID()).id());
        assertEquals("C-02", inventory.reserveCompatibleSpot(
                VehicleType.MOTORCYCLE, UUID.randomUUID()).id());
    }

    @Test
    void skipsOccupiedSpotsAndRejectsWhenNoCompatibleSpaceRemains() {
        InMemorySpotInventory inventory = new InMemorySpotInventory(List.of(
                new ParkingSpot("C-01", SpotType.COMPACT),
                new ParkingSpot("L-01", SpotType.LARGE)));
        inventory.reserveCompatibleSpot(VehicleType.CAR, UUID.randomUUID());

        assertThrows(NoCompatibleSpotAvailableException.class,
                () -> inventory.reserveCompatibleSpot(VehicleType.MOTORCYCLE, UUID.randomUUID()));
        assertEquals("L-01", inventory.reserveCompatibleSpot(
                VehicleType.BUS, UUID.randomUUID()).id());
    }

    @Test
    void releaseRequiresMatchingTicketAndMakesTheSpotAvailableAgain() {
        InMemorySpotInventory inventory = new InMemorySpotInventory(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)));
        UUID ticketId = UUID.randomUUID();
        ParkingSpot reserved = inventory.reserveCompatibleSpot(VehicleType.CAR, ticketId);

        assertThrows(SpotNotOccupiedByTicketException.class,
                () -> inventory.releaseSpot(reserved.id(), UUID.randomUUID()));
        inventory.releaseSpot(reserved.id(), ticketId);
        assertEquals("C-01", inventory.reserveCompatibleSpot(
                VehicleType.CAR, UUID.randomUUID()).id());
    }

    @Test
    void rejectsUnknownAndDuplicateSpotIds() {
        InMemorySpotInventory inventory = new InMemorySpotInventory(
                List.of(new ParkingSpot("C-01", SpotType.COMPACT)));

        assertThrows(InvalidSpotException.class,
                () -> inventory.releaseSpot("missing", UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> new InMemorySpotInventory(List.of(
                new ParkingSpot("C-01", SpotType.COMPACT),
                new ParkingSpot("C-01", SpotType.LARGE))));
    }

    @Test
    void rejectsSpotsThatAreAlreadyOccupiedAtInitialization() {
        ParkingSpot occupied = new ParkingSpot("C-01", SpotType.COMPACT);
        occupied.reserve(UUID.randomUUID(), VehicleType.CAR);

        assertThrows(IllegalArgumentException.class,
                () -> new InMemorySpotInventory(List.of(occupied)));
    }
}
