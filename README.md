# Smart Parking Lot Rate Calculator

This repository contains the approved design and phased Java implementation for ticket issuance, compatible spot assignment, and lowest-valid-fare calculation. Payment processing is explicitly out of scope.

The implementation follows the approved design: keep the system simple, use Java 17 and JDK production libraries, separate domain behavior from application orchestration, and verify behavior with JUnit.

## Project status

The domain, pricing policies, in-memory facility, and interactive console demo are implemented. The console supports multiple vehicle entries and exits in one run; tickets and parking state are held in memory.

## Build and tests

Run the unit test suite with:

```sh
./mvnw test
```

Compile and start the interactive demo with:

```sh
./mvnw compile
java -cp target/classes com.sahaj.smartparkinglot.Main
```

Use the menu to enter a vehicle, exit by ticket ID, show active tickets and available spots, or quit. For entry, provide a vehicle (`MOTORCYCLE`, `CAR`, or `BUS`), an ISO-8601 UTC entry instant (for example `2026-01-05T16:00:00Z`), and a loyalty tier (`NONE`, `SILVER`, `GOLD`, or `PLATINUM`; blank defaults to `NONE`). Exit requires the ticket ID and an ISO-8601 UTC exit instant. Bundled federal holiday data is available for 2025-2029.

## Confirmed decisions

- Vehicle categories are Motorcycle, Car, and Bus, with car-rate multipliers of 0.8, 1.0, and 2.0 respectively.
- The only spot compatibility pairs are Motorcycle→Compact, Car→Compact, and Bus→Large.
- Hourly base pricing applies every day. The peak surcharge is limited to weekdays, excluding US federal holidays.
- Early Bird and Night Owl are eligible only on weekdays excluding US federal holidays.
- Use the US Office of Personnel Management (OPM) federal holiday schedule, including observed weekdays.
- Early Bird and Night Owl remain eligible when loyalty is `None`; in that case, the discount is 0%.
- Store the customer's loyalty tier on the ticket; default it to `None`.
- Vehicle multipliers apply to special-rate fees as well as hourly fees.
- Invalid inputs should raise an appropriate exception.
- Calculate the selected fare and round the final amount to cents using half-up rounding.
- If fares tie, prefer Early Bird, then Night Owl, then Standard Hourly.
- If a stay exceeds 24 hours, only Standard Hourly is evaluated.
- Default the facility timezone to `America/Los_Angeles` for the current implementation. Payment is not part of this plan.
- Require a ticket to exit once only and release its spot after successful exit.
- Reject entry with an appropriate message when no compatible spot is available.
- Keep ticket and spot state in memory; surviving process restarts is not required.
- For Night Owl, require both entry and exit dates to be weekdays excluding federal holidays.
- Determine fare ties from exact amounts; values that only tie after cent rounding do not use tie precedence.

## Source documents

- `Assignment_Instructions.pdf` — assignment design, testing, technology, and AI-transparency requirements supplied by the user.
- `The_Smart_Parking_Lot_v2.pdf` — parking and pricing requirements supplied by the user.

The source PDFs were provided from `/home/shyamsrk/` and are not copied into this repository.

## Architecture clarification status

The Stage 4 design questions have been resolved and recorded. Implementation remains paused pending the next instruction.

## Architecture diagrams

These diagrams illustrate the system boundary, component responsibilities, and entry/exit workflows documented in [`docs/system-architecture.md`](docs/system-architecture.md).

![Smart Parking Lot system context](docs/diagrams/system-context.svg)

![Smart Parking Lot component architecture](docs/diagrams/component-architecture.svg)

![Smart Parking Lot entry and exit workflows](docs/diagrams/entry-exit-workflows.svg)

## Design decisions and trade-offs

The Stage 2 design is approved; the Stage 4 system architecture records the user's resolved choices:

- Keep entry/exit orchestration, spot inventory, and each pricing policy separate; this is clearer to test than a single service with interleaved conditionals.
- Use three explicit policy implementations behind a small shared contract rather than a configurable rules engine; there are only three fixed policies in scope.
- Use decimal-safe money arithmetic and round once at the final-fare boundary rather than using binary floating point or rounding each hourly segment.
- Use the documented `America/Los_Angeles` facility-timezone default for the current implementation, with the timezone kept configurable.
- Use the US OPM federal holiday schedule, including observed weekdays, as the holiday-calendar source.
- Keep the domain plan independent of a database, web API, UI, payment framework, and selected language, as required by the assignment; use in-memory state for this assignment.
- Target Java 17, Maven Wrapper 3.9.9, JUnit Jupiter 5.11.4, and base package `com.sahaj.smartparkinglot`; production code should use JDK standard libraries only.
- Load versioned per-year OPM holiday data from repository resources; accept `Instant` timestamps only; serialize concurrent state transitions in memory.
- Assign spots by ascending canonical spot ID, generate ticket IDs with JDK UUIDs, and report typed domain/application exceptions for callers to format.

See [`docs/specification.md`](docs/specification.md) for the functional specification and acceptance criteria, [`docs/design.md`](docs/design.md) for the approved domain model, [`docs/system-architecture.md`](docs/system-architecture.md) for the Java system design and future enhancements, [`docs/test-plan.md`](docs/test-plan.md) for the test strategy, [`docs/user-acceptance-tests.md`](docs/user-acceptance-tests.md) for acceptance scenarios and results, and [`ai-chat-history.md`](ai-chat-history.md) for the user prompts and decisions recorded for this project.
