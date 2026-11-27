package com.sahaj.smartparkinglot;

import com.sahaj.smartparkinglot.application.EntryService;
import com.sahaj.smartparkinglot.application.ExitService;
import com.sahaj.smartparkinglot.domain.FareQuote;
import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.ParkingSpot;
import com.sahaj.smartparkinglot.domain.ParkingTicket;
import com.sahaj.smartparkinglot.domain.SpotType;
import com.sahaj.smartparkinglot.domain.VehicleType;
import com.sahaj.smartparkinglot.domain.exception.InvalidParkingStayException;
import com.sahaj.smartparkinglot.domain.exception.InvalidPricingConfigurationException;
import com.sahaj.smartparkinglot.domain.exception.NoCompatibleSpotAvailableException;
import com.sahaj.smartparkinglot.domain.exception.TicketAlreadyExitedException;
import com.sahaj.smartparkinglot.domain.exception.TicketNotFoundException;
import com.sahaj.smartparkinglot.domain.pricing.EarlyBirdPolicy;
import com.sahaj.smartparkinglot.domain.pricing.NightOwlPolicy;
import com.sahaj.smartparkinglot.domain.pricing.RateCalculator;
import com.sahaj.smartparkinglot.domain.pricing.StandardHourlyPolicy;
import com.sahaj.smartparkinglot.domain.time.PricingContext;
import com.sahaj.smartparkinglot.infrastructure.calendar.OpmFederalHolidayCalendar;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemoryParkingFacility;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemorySpotInventory;
import com.sahaj.smartparkinglot.infrastructure.memory.InMemoryTicketRegistry;
import com.sahaj.smartparkinglot.infrastructure.memory.UuidTicketIdGenerator;

import java.io.PrintStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;

public final class Main {
    private static final int COMPACT_SPOT_COUNT = 10;
    private static final int LARGE_SPOT_COUNT = 5;

    private Main() {
    }

    public static void main(String[] args) {
        run(new Scanner(System.in), System.out);
    }

    static void run(Scanner input, PrintStream output) {
        run(input, output, createFacility());
    }

    static void run(Scanner input, PrintStream output, InMemoryParkingFacility facility) {
        output.println("Smart Parking Lot");
        EntryService entryService = facility;
        ExitService exitService = facility;

        while (true) {
            output.println();
            output.println("1) Enter vehicle");
            output.println("2) Exit by ticket ID");
            output.println("3) Show parking status");
            output.println("0) Quit");
            output.print("Selection: ");
            output.flush();
            if (!input.hasNextLine()) {
                return;
            }

            switch (input.nextLine().trim()) {
                case "1" -> enterVehicle(input, output, entryService);
                case "2" -> exitVehicle(input, output, exitService);
                case "3" -> showStatus(output, facility);
                case "0" -> {
                    output.println("Goodbye.");
                    return;
                }
                default -> output.println("Input error: choose 1, 2, 3, or 0.");
            }
        }
    }

    private static void enterVehicle(Scanner input, PrintStream output, EntryService entryService) {
        VehicleType vehicleType;
        Instant entryAt;
        LoyaltyTier loyaltyTier;
        try {
            vehicleType = parseVehicleType(prompt(input, output,
                    "Vehicle (MOTORCYCLE/CAR/BUS): "));
            entryAt = parseInstant(prompt(input, output,
                    "Entry instant (ISO-8601 UTC, e.g. 2026-01-05T16:00:00Z): "), "entry");
            loyaltyTier = parseLoyaltyTier(prompt(input, output,
                    "Loyalty tier (NONE/SILVER/GOLD/PLATINUM; blank for NONE): "));
        } catch (IllegalArgumentException exception) {
            output.println("Input error: " + exception.getMessage());
            return;
        }

        try {
            ParkingTicket ticket = entryService.enter(vehicleType, entryAt, loyaltyTier);
            output.println("Ticket ID: " + ticket.id());
            output.println("Assigned spot: " + ticket.spotId());
        } catch (NoCompatibleSpotAvailableException exception) {
            output.println("Unable to park: no compatible spot is available.");
        }
    }

    private static void exitVehicle(Scanner input, PrintStream output, ExitService exitService) {
        UUID ticketId;
        Instant exitAt;
        try {
            ticketId = parseTicketId(prompt(input, output, "Ticket ID: "));
            exitAt = parseInstant(prompt(input, output,
                    "Exit instant (ISO-8601 UTC, e.g. 2026-01-05T17:00:00Z): "), "exit");
        } catch (IllegalArgumentException exception) {
            output.println("Input error: " + exception.getMessage());
            return;
        }

        try {
            FareQuote quote = exitService.exit(ticketId, exitAt);
            output.printf("Fare: $%s (%s)%n", quote.amount(), quote.policy());
        } catch (TicketNotFoundException exception) {
            output.println("Unable to exit: ticket was not found.");
        } catch (TicketAlreadyExitedException exception) {
            output.println("Unable to exit: ticket has already exited.");
        } catch (InvalidParkingStayException exception) {
            output.println("Unable to exit: exit instant must be after entry instant.");
        } catch (InvalidPricingConfigurationException exception) {
            output.println(
                    "Unable to calculate fare: holiday data is available for 2025 through 2029.");
        }
    }

    private static void showStatus(PrintStream output, InMemoryParkingFacility facility) {
        List<ParkingTicket> activeTickets = facility.activeTickets();
        long occupiedCompact = activeTickets.stream()
                .filter(ticket -> ticket.vehicleType().compatibleSpotType() == SpotType.COMPACT)
                .count();
        long occupiedLarge = activeTickets.size() - occupiedCompact;

        output.println("Parking status");
        output.printf("Compact spots available: %d/%d%n",
                COMPACT_SPOT_COUNT - occupiedCompact, COMPACT_SPOT_COUNT);
        output.printf("Large spots available: %d/%d%n",
                LARGE_SPOT_COUNT - occupiedLarge, LARGE_SPOT_COUNT);
        output.println("Active tickets: " + activeTickets.size());
        for (ParkingTicket ticket : activeTickets) {
            output.printf("%s | %s | %s | entered %s%n",
                    ticket.id(), ticket.vehicleType(), ticket.spotId(), ticket.entryAt());
        }
    }

    private static String prompt(Scanner input, PrintStream output, String label) {
        output.print(label);
        output.flush();
        if (!input.hasNextLine()) {
            throw new IllegalArgumentException("input ended before all values were provided.");
        }
        return input.nextLine().trim();
    }

    private static VehicleType parseVehicleType(String value) {
        try {
            return VehicleType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("vehicle must be MOTORCYCLE, CAR, or BUS.");
        }
    }

    private static LoyaltyTier parseLoyaltyTier(String value) {
        if (value.isBlank()) {
            return LoyaltyTier.NONE;
        }
        try {
            return LoyaltyTier.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("loyalty tier must be NONE, SILVER, GOLD, or PLATINUM.");
        }
    }

    private static Instant parseInstant(String value, String label) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    label + " instant must use ISO-8601 UTC format, for example 2026-01-05T16:00:00Z.");
        }
    }

    private static UUID parseTicketId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("ticket ID must be a UUID.");
        }
    }

    private static InMemoryParkingFacility createFacility() {
        return new InMemoryParkingFacility(
                new InMemorySpotInventory(createSpots()),
                new InMemoryTicketRegistry(),
                new UuidTicketIdGenerator(),
                new RateCalculator(
                        new PricingContext(new OpmFederalHolidayCalendar()),
                        new StandardHourlyPolicy(),
                        new EarlyBirdPolicy(),
                        new NightOwlPolicy()));
    }

    private static List<ParkingSpot> createSpots() {
        List<ParkingSpot> spots = new ArrayList<>();
        addSpots(spots, "C", SpotType.COMPACT, COMPACT_SPOT_COUNT);
        addSpots(spots, "L", SpotType.LARGE, LARGE_SPOT_COUNT);
        return spots;
    }

    private static void addSpots(List<ParkingSpot> spots, String prefix, SpotType type, int count) {
        for (int number = 1; number <= count; number++) {
            spots.add(new ParkingSpot("%s-%02d".formatted(prefix, number), type));
        }
    }
}
