# Stage 4: System Architecture

## Status and scope

Proposed system design, prepared at the user's request. It refines the approved business specification and domain model for the selected Java technology stack. This document does not start implementation; the next action is paused for user direction.

The design covers ticket entry, in-memory spot assignment, fee calculation at exit, and the test/build structure. Payment, a web API, GUI, database, and multi-process deployment remain out of scope.

## Technology baseline

| Concern | Selected / available |
|---|---|
| Language/runtime | Java 17; OpenJDK 17.0.21.1 is installed |
| Build | Maven; Maven 3.9.9 is installed |
| Tests | JUnit Jupiter; 5.11.4 artifacts are present in the local Maven cache |
| Production dependencies | JDK standard library only |
| Project build configuration | No `pom.xml` exists yet; plugin and dependency versions are not pinned |

Java 17 and Maven/JUnit are the requested implementation stack. JUnit 5.11.4 is an available version, not yet pinned in a project build. No dependency was downloaded and no build or test was run during this design stage.

## System context and shape

The smallest proposed deployment is a single Java process with a modular domain core and in-memory state. This avoids introducing a database, REST framework, or dependency-injection framework for a fixed, small domain.

```text
Caller / future entry point
       |
       v
Application use cases
  EntryService     ExitService
       |                |
       v                v
In-memory facility state   RateCalculator
  SpotInventory           /      |       \
  TicketRegistry        Hourly  EarlyBird  NightOwl
       |                   \      |       /
       +-------------------- PricingContext
                               ZoneId + OPM calendar
```

The caller/entry point is intentionally not defined. It may be a small demonstration harness later, but no CLI, GUI, or network API is assumed by this design.

## Component responsibilities and dependencies

### Application layer

- **`EntryService`** accepts vehicle type, entry instant, and loyalty tier (default `None`). It validates the request, obtains a compatible spot, creates a ticket, and returns it.
- **`ExitService`** accepts a ticket ID and exit instant. It rejects missing or already-exited tickets, validates the stay, requests a fare quote, and on successful calculation marks the ticket exited and releases its spot.
- Application services coordinate domain operations. They do not contain rate arithmetic or expose mutable inventory collections.

### Domain layer

- **`ParkingFacility` / in-memory state owner** contains the configured spots and active tickets. It owns state transitions that must remain consistent.
- **`SpotInventory`** checks the fixed vehicle/spot matrix, reserves an available compatible spot, and releases a spot only for the matching exited ticket.
- **`ParkingTicket`** holds ID, entry instant, vehicle type, assigned spot ID, loyalty tier, and `ACTIVE`/`EXITED` state. A successful exit is allowed once.
- **`ParkingStay`** is an immutable calculation input derived from an active ticket and a later exit instant.
- **`RateCalculator`** evaluates every policy applicable to the stay, selects the least exact amount, applies the confirmed exact-tie precedence, and rounds the winning amount once.
- **`StandardHourlyPolicy`, `EarlyBirdPolicy`, and `NightOwlPolicy`** contain their own eligibility and price calculation rules and return no candidate when inapplicable.
- **`Money` and `FareQuote`** carry exact USD amounts and selected-policy details; money arithmetic must not use binary floating point.

### Calendar and time boundary

- Represent entry/exit instants as `java.time.Instant`; calculate elapsed duration from instants.
- Use `java.time.ZoneId` with the facility default `America/Los_Angeles` to derive local dates and times for rate windows.
- Supply holiday membership through a `FederalHolidayCalendar` dependency backed by the US OPM schedule (including observed weekdays). The schedule's exact update/version mechanism is still open.
- Use half-open time intervals for overlap checks. For an hourly segment, evaluate its entire elapsed one-hour interval against all relevant facility-local dates and peak windows.
- If an external caller can supply local wall times instead of instants, it must provide an offset or a separately approved DST ambiguity rule; the application contract is not yet fixed.

## Entry workflow

1. Validate the vehicle type and request data.
2. Ask the inventory to atomically reserve a compatible available spot.
3. If none exists, return an explicit no-compatible-spot error and issue no ticket.
4. Create an active ticket with a unique ID, entry instant, vehicle, spot, and tier (`None` when omitted).
5. If ticket creation fails after reservation, release the reservation before propagating the error.

The exact spot selection rule among multiple compatible spaces is not defined. A deterministic first-available rule is a simple candidate, but it is not treated as an approved requirement here.

## Exit workflow

1. Find the ticket and require its state to be `ACTIVE`.
2. Validate that the exit instant is after entry; create an immutable `ParkingStay`.
3. Use a `PricingContext` containing the facility zone and OPM calendar to evaluate the policies.
4. Choose the exact minimum candidate; use Early Bird, Night Owl, Standard Hourly for exact ties; round the winner half-up to cents.
5. Only after a valid quote exists, mark the ticket exited and release its assigned spot.
6. Return the quote. Do not initiate payment.

If steps 5's state transition cannot complete, the ticket and inventory must remain consistent. The in-memory state owner should expose one guarded exit operation rather than allowing callers to mutate ticket and spot independently.

## Proposed Java project layout

The base package name is intentionally left undecided; use a single project-specific root package once approved.

```text
pom.xml
src/
  main/java/<base-package>/
    application/       EntryService, ExitService
    domain/            ParkingFacility, ParkingTicket, ParkingSpot, ParkingStay
    domain/pricing/    Money, FareQuote, RateCalculator, RatePolicy, policy classes
    domain/time/       PricingContext, FederalHolidayCalendar
    infrastructure/   in-memory inventory/state and OPM calendar data adapter
  test/java/<base-package>/
    application/       entry/exit flow tests
    domain/            ticket, spot, stay tests
    domain/pricing/    hourly, special-policy, selection, and money tests
```

Use Maven's Java 17 compiler release setting and a JUnit Jupiter test dependency. Production code should have no third-party runtime dependency. Maven's test lifecycle should run the JUnit suite; exact Maven plugin versions and the JUnit version to pin are build-file decisions for the implementation stage.

## State consistency and concurrency

In-memory state is accepted for this assignment, so a process restart loses active tickets and occupancy. Entry allocation and exit/release still need atomic state transitions to prevent assigning one spot twice or leaving an exited ticket's spot occupied.

The current deployment concurrency model has not been specified. A straightforward implementation can guard compound in-memory state transitions with one lock using JDK synchronization; this is a design proposal, not a requirement for a particular locking API. If the application is strictly single-threaded, the implementation can keep the same aggregate boundary without concurrency machinery.

## Error behavior

Use explicit domain/application failures and do not silently substitute defaults other than the confirmed loyalty and timezone defaults. Candidate exception categories:

- invalid entry or exit data;
- unknown or already-exited ticket;
- no compatible available spot;
- invalid vehicle/spot classification;
- missing or invalid pricing configuration.

Exact exception class names and user-facing message format should be chosen with the application entry point. The no-space outcome is already fixed: reject entry and issue no ticket.

## Design choices and alternatives

- **Single-process modular application** over distributed services: fewer moving parts, aligns with in-memory state and no API/database requirement; does not provide restart durability or horizontal scale.
- **Explicit policy classes** over a generic rules engine: keeps the three fixed rates easy to inspect and test; adding a future policy means adding one class and registering it.
- **JDK `java.time` and `BigDecimal`** over third-party date/money libraries: standard-library constraint, timezone-aware instants, and decimal-safe fee arithmetic.
- **Manual dependency wiring** over a DI framework: only a few services and policies need construction.
- **OPM calendar dependency** over live holiday lookups: deterministic offline calculations; schedule distribution/refresh needs a follow-up decision.

## Decisions to confirm before implementation

1. Approve this single-process architecture and the proposed Java package/component boundaries.
2. Confirm how the OPM schedule is supplied and versioned (for example, a checked-in/versioned data resource versus another offline source).
3. Confirm whether entry/exit operations can be concurrent; if so, approve atomic synchronization around the in-memory facility state.
4. Choose the spot ordering rule when several compatible spaces are free.
5. Confirm the project base package and the JUnit Jupiter version to pin. JUnit 5.11.4 is available locally.
6. Decide whether callers will supply `Instant` values only or may supply local date-times; local input needs an explicit DST ambiguity rule.

No implementation starts until the next instruction.
