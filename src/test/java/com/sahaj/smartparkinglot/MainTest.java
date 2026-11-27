package com.sahaj.smartparkinglot;

import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.ParkingSpot;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.SpotType;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.pricing.EarlyBirdPolicy;
import com.sahaj.smartparkinglot.domain.pricing.NightOwlPolicy;
import com.sahaj.smartparkinglot.domain.pricing.RateCalculator;
import com.sahaj.smartparkinglot.domain.pricing.StandardHourlyPolicy;
import com.sahaj.smartparkinglot.domain.time.PricingContext;
import com.sahaj.smartparkinglot.infrastructure.calendar.OpmFederalHolidayCalendar;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemoryParkingFacility;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemorySpotInventory;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemoryTicketRegistry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    private static final UUID TICKET_ID =
            UUID.fromString("6d10d96b-8173-4aeb-bfaa-5c53ee32464e");

    @Test
    void supportsEntryExitStatusAndQuitInOneSession() {
        String input = """
                1
                CAR
                2026-01-05T16:00:00Z
                GOLD
                3
                2
                6d10d96b-8173-4aeb-bfaa-5c53ee32464e
                2026-01-05T17:00:00Z
                3
                0
                """;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Main.run(new Scanner(input), new PrintStream(output), createFacility());

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Ticket ID: " + TICKET_ID));
        assertTrue(result.contains("Assigned spot: C-01"));
        assertTrue(result.contains("Fare: $7.50 (STANDARD_HOURLY)"));
        assertTrue(result.contains("Compact spots available: 9/10"));
        assertTrue(result.contains("Compact spots available: 10/10"));
        assertTrue(result.contains("Active tickets: 1"));
        assertTrue(result.contains("Active tickets: 0"));
        assertTrue(result.contains("Goodbye."));
    }

    @Test
    void reportsInvalidInputAndContinuesToTheMenu() {
        String input = """
                1
                TRUCK
                0
                """;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Main.run(new Scanner(input), new PrintStream(output), createFacility());

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Input error: vehicle must be MOTORCYCLE, CAR, or BUS."));
        assertTrue(result.contains("Goodbye."));
        assertTrue(!result.contains("Ticket ID: "));
    }

    @Test
    void reportsRepeatedExitWithoutCrashingTheMenu() {
        String input = """
                1
                CAR
                2026-01-05T16:00:00Z
                NONE
                2
                6d10d96b-8173-4aeb-bfaa-5c53ee32464e
                2026-01-05T17:00:00Z
                2
                6d10d96b-8173-4aeb-bfaa-5c53ee32464e
                2026-01-05T17:00:00Z
                0
                """;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Main.run(new Scanner(input), new PrintStream(output), createFacility());

        assertTrue(output.toString(StandardCharsets.UTF_8)
                .contains("Unable to exit: ticket has already exited."));
    }

    private static InMemoryParkingFacility createFacility() {
        List<ParkingSpot> spots = new ArrayList<>();
        for (int number = 1; number <= 10; number++) {
            spots.add(new ParkingSpot("C-%02d".formatted(number), SpotType.COMPACT));
        }
        for (int number = 1; number <= 5; number++) {
            spots.add(new ParkingSpot("L-%02d".formatted(number), SpotType.LARGE));
        }

        return new InMemoryParkingFacility(
                new InMemorySpotInventory(spots),
                new InMemoryTicketRegistry(),
                () -> TICKET_ID,
                new RateCalculator(
                        new PricingContext(new OpmFederalHolidayCalendar()),
                        new StandardHourlyPolicy(),
                        new EarlyBirdPolicy(),
                        new NightOwlPolicy()));
    }
}
