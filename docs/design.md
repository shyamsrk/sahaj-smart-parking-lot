# Stage 2: Architecture and Domain Model

## Status

Stage 2 design accepted. It follows the accepted Stage 1 specification and records the confirmed operational rules; it does not select a programming language, database, or deployment style.

## Architectural shape

Use a small domain core with explicit entry and exit use cases. Keep time/calendar and inventory decisions at the edges, and keep the pricing policies deterministic and side-effect free.

```text
Entry request
    |
    v
EntryService ---> SpotAllocator ---> SpotInventory
    |                                  |
    +---------- issues ParkingTicket <-+

Exit request (ticket, exit instant)
    |
    v
ExitService ---> Stay validation ---> RateCalculator
                                      |  |  |
                                      |  |  +--> NightOwlPolicy
                                      |  +-----> EarlyBirdPolicy
                                      +--------> StandardHourlyPolicy
                                      |
                                      v
                             lowest valid FareQuote
```

### Components

- **EntryService:** Coordinates an entry request, compatible-spot allocation, and ticket creation. It owns orchestration, not rates.
- **SpotAllocator:** Enforces the fixed compatibility relation and asks the inventory for an available compatible spot.
- **SpotInventory:** Encapsulates the spot collection and its occupancy/reservation state. It exposes operations rather than its mutable collection.
- **ExitService:** Accepts a ticket and exit timestamp, validates the stay, and delegates price selection. Payment is deliberately not called.
- **RateCalculator:** Evaluates every configured policy and selects the lowest valid candidate. It owns the tie precedence but not each policy's pricing rules.
- **Pricing policies:** `StandardHourlyPolicy`, `EarlyBirdPolicy`, and `NightOwlPolicy` each determine applicability and calculate their own exact candidate amount.
- **PricingContext:** Supplies the facility timezone and a federal-holiday calendar to policy evaluation. The current implementation defaults the timezone to `America/Los_Angeles`.
- **FederalHolidayCalendar:** Answers whether a local date is a federal holiday, including observed weekdays. Use the US Office of Personnel Management (OPM) federal holiday schedule; its refresh/version policy remains to be selected.

The policy set is fixed and small, so a simple collection of policy objects is preferred over a plug-in registry or externally configurable rules engine. A shared time-window predicate and shared loyalty-discount calculation avoid duplicating the common Early Bird/Night Owl behavior.

## Entry and exit flows

### Entry

1. Validate vehicle classification and required request data.
2. Find an available spot compatible with the vehicle.
3. If no compatible spot is available, reject entry with an appropriate message and do not issue a ticket.
4. Otherwise, reserve that spot and issue a ticket containing its identifier, entry instant, vehicle type, and loyalty tier (`None` by default).
5. Return the ticket to the caller.

Reservation and ticket issuance must not leave a reserved spot without a usable ticket. For the in-memory scope, encapsulate both operations in the entry use case and release the reservation if ticket creation fails.

### Exit and fare calculation

1. Resolve the ticket and validate required fields and `exitAt > entryAt`.
2. Construct a `ParkingStay` from the ticket's entry data and the supplied exit instant.
3. Convert timestamps to facility-local calendar/time values for window and holiday checks. Preserve elapsed duration independently of the local wall clock.
4. Ask each policy for either no candidate (inapplicable) or a `FareCandidate`.
5. Select the least expensive exact candidate; use Early Bird, Night Owl, then Standard Hourly only when exact amounts tie. Amounts that become equal only after rounding are not ties.
6. Round the selected amount once to cents using half-up rounding and return the final quote.

For stays over 24 elapsed hours, the calculator excludes both specials and evaluates Standard Hourly only. On a successful exit, mark the ticket exited once and release its spot. Reject a second exit for the same ticket. A successful exit does not trigger a payment operation.

## Domain objects and responsibilities

| Object | Kind and state | Behavior / invariants |
|---|---|---|
| `VehicleType` | Enum: Motorcycle, Car, Bus | Supplies its fixed car-rate multiplier. |
| `SpotType` | Enum: Compact, Large | Identifies the physical spot category. |
| `ParkingSpot` | Entity: identifier, type, occupancy/reservation state | Encapsulates reserve/release transitions; cannot be assigned to an incompatible vehicle. |
| `SpotInventory` | Aggregate/service owning spots | Finds and reserves an available compatible spot without exposing mutable storage. |
| `ParkingTicket` | Entity: ticket ID, entry instant, vehicle type, spot ID, loyalty tier, active/exited state | Captures entry facts; loyalty defaults to `None`; permits a single successful exit. |
| `LoyaltyTier` | Enum: None, Silver, Gold, Platinum | Maps to 0%, 10%, 20%, or 30% discount respectively. |
| `ParkingStay` | Value object: ticket facts plus exit instant | Requires exit after entry; provides elapsed duration and facility-local dates/times via context. |
| `Money` | Value object: exact amount in USD | Uses decimal-safe arithmetic; performs half-up rounding only at the final-fare boundary. |
| `FareCandidate` | Value object: policy identity and exact USD amount | Preserves an unrounded policy result for best-fare comparison. |
| `FareQuote` | Value object: policy identity, exact candidate amount, final rounded amount | Carries the reason/policy selected for a customer-visible calculation result. |
| `RatePolicy` | Behavior contract | Given a valid stay and pricing context, returns an optional `FareCandidate`. |
| `PricingContext` | Value/configuration object | Holds the facility timezone (default `America/Los_Angeles` for the current implementation) and OPM holiday-calendar dependency. |

Domain objects own invariants and domain behavior; use cases coordinate them. Avoid exposing mutable ticket or spot state directly. The model is intentionally independent of database, web, UI, and payment frameworks.

## Pricing-policy design

### Shared policy contract

Each policy receives a validated `ParkingStay` and `PricingContext`, and returns an optional exact `FareCandidate`. It does not choose the winner or round an intermediate amount. `RateCalculator` compares candidates by exact amount and creates one final `FareQuote` for the selected candidate. This lets the policy classes remain independently understandable and testable.

### Standard Hourly

- Determine the number of segments by ceiling elapsed duration to whole hours.
- Build each segment as a one-hour interval from the entry instant (the last segment may extend beyond exit, as required by duration ceiling).
- Apply the progressive Car rate by segment index.
- For each segment, determine whether any portion intersects either local peak window on a weekday that is not a federal holiday; multiply that entire segment's base rate by 1.5 on overlap.
- Sum exact segment amounts, then apply the vehicle multiplier.

### Early Bird and Night Owl

- Share a small time-window/calendar eligibility helper and a loyalty-discount mapping.
- Early Bird checks its same-date entry and exit windows; Night Owl checks its entry window and next-consecutive-date exit window.
- Check the confirmed weekday/federal-holiday eligibility on the required dates. Night Owl requires both entry and exit dates to be weekdays excluding OPM federal holidays.
- Exclude both policies for stays longer than 24 elapsed hours.
- Apply the tier discount to the policy's Car base rate, then the vehicle multiplier. Do not round until the selected quote is finalized.

### Best-value selection

Compare exact candidate amounts before final rounding; use the documented policy precedence only for exact equal amounts. Amounts that become equal only after cent rounding are not ties.

## Error behavior and boundaries

- Invalid or missing required inputs fail explicitly with domain-appropriate exceptions; no success-shaped fallback is allowed.
- Unknown vehicle or spot categories are rejected rather than assigned a default multiplier or compatibility.
- No compatible available spot rejects entry with an appropriate message and does not issue a ticket.
- Standard Hourly is always an applicable valid-stay policy, so a valid stay should have at least one candidate.
- All local time windows and holiday checks use the facility timezone, defaulted to `America/Los_Angeles` for the current implementation. Calendar data uses the OPM holiday schedule.

## Deliberate trade-offs

- **Three explicit policies instead of a generic rules engine:** simpler to reason about and test for the fixed set in the assignment; adding policies later would require implementing the same small contract.
- **Exact decimal arithmetic instead of binary floating point:** avoids currency drift and supports the required final half-up rounding.
- **Policy objects plus one calculator instead of condition chains in a monolith:** policy behavior remains cohesive while exhaustive evaluation and minimum selection stay centralized.
- **No persistence or API layer in this design:** the assignment prohibits databases and heavy frameworks and focuses on the domain. In-memory ticket/spot state is accepted for this assignment; state will not survive process restarts.
- **Timezone and holiday calendar configuration:** keeps the pricing rules deterministic; the initial choices are `America/Los_Angeles` and the OPM schedule.
- **In-memory inventory and tickets:** sufficient for the assignment scope and avoids prohibited databases, at the explicit cost of losing state across process restarts.

## Decisions resolved after Stage 2

The Stage 4 system architecture records the later decisions for holiday-resource versioning, Instant-only time input, concurrency, spot ordering, package/build settings, ticket IDs, and error types. See [`system-architecture.md`](system-architecture.md).
