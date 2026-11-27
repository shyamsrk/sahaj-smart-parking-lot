# Stage 3: Test Strategy and Scenario Matrix

## Status and scope

This Stage 3 test plan is approved. It defines what should be verified before an implementation is accepted; it does not add executable code. JUnit Jupiter is the selected test framework; 5.11.4 is available locally but is not yet pinned in a Maven project. No production implementation exists yet, so no tests have been run.

The test approach uses JUnit Jupiter with Maven. Pricing and domain tests should be unit-level and deterministic; a small number of in-memory flow tests verify interaction between ticketing, spot allocation, and pricing.

## Test design principles

- Test observable behavior and business boundaries rather than private implementation details.
- Supply timezone-aware timestamps and a fixed facility timezone (`America/Los_Angeles` for the current implementation).
- Inject a deterministic holiday-calendar fixture based on the US OPM schedule; tests must not make live network calls or depend on the date they run.
- Use exact decimal values in expected fares. Verify half-up rounding separately using values that exercise the rounding boundary.
- Use in-memory inventory and test doubles for policies/calendar collaborators where needed.
- Keep daylight-saving ambiguous/nonexistent local-time expectations out of the acceptance suite until the unresolved rule is decided.

## Unit test scenarios

### Vehicle, spot, and ticket domain

| Scenario | Expected result |
|---|---|
| Vehicle multipliers for Motorcycle, Car, and Bus | 0.8, 1.0, and 2.0 respectively |
| Compatibility lookup for each supported vehicle | Motorcycle→Compact, Car→Compact, Bus→Large |
| Inventory contains a compatible available spot | Reserve and return a compatible spot; mark it occupied |
| Inventory contains only incompatible or occupied spots | Reject entry with an appropriate no-compatible-spot message; do not issue a ticket or change occupancy |
| Ticket issued without a loyalty tier | Store `None` |
| Ticket issued with each supplied loyalty tier | Preserve the tier on the ticket |
| Successful exit | Mark the ticket exited once and release its spot |
| Attempt a second exit | Reject it; do not calculate/charge a second fare or release another spot |
| Invalid vehicle, missing ticket, or malformed required input | Raise an appropriate exception; do not return a success-shaped result |

### Stay validation and time boundaries

| Scenario | Expected result |
|---|---|
| Exit equals entry or precedes entry | Reject the stay |
| Valid timezone-aware entry and later exit | Construct a valid stay with positive elapsed duration |
| Convert an unambiguous instant to facility-local date/time | Use `America/Los_Angeles` local values for policy windows |
| Ambiguous/nonexistent local time during a daylight-saving transition | Keep blocked from definitive expected-result tests until the rule is confirmed |

### Standard Hourly policy

| Scenario | Expected result |
|---|---|
| Stay of exactly one hour, outside peak windows | One segment at the $5.00 Car base rate |
| Positive stay shorter than one hour | One segment |
| Stay of one hour plus a positive remainder | Two segments |
| Stay of exactly two hours, with both segments outside peak windows | $5.00 first segment plus $3.00 second segment |
| Stay exceeding two hours | Apply $2.00 to every segment after the second |
| Weekday segment 06:30–07:30 | Entire first segment is peak; Car rate is $7.50 |
| Weekday segment 07:30–08:30 | Entire second segment is peak; Car rate is $4.50 |
| Segment ends exactly at 07:00 | No peak overlap |
| Segment begins exactly at 07:00 or 16:00 | Peak applies |
| Segment begins exactly at 10:00 or 19:00 | End of the peak window is exclusive; no peak overlap |
| Segment overlaps the peak window before ending exactly at 10:00 or 19:00 | Peak applies because the segment overlaps before the exclusive endpoint |
| Weekday segment partially overlaps 09:30–10:30 or 15:30–16:30 | Entire segment receives the 1.5 multiplier |
| Weekend or OPM federal/observed holiday segment within a peak clock window | No peak surcharge; base hourly rate still applies |
| Hourly segments span a date boundary | Evaluate each segment against local date and local peak windows |
| Motorcycle and Bus hourly totals | Apply 0.8 and 2.0 multipliers respectively |
| Stay longer than 24 hours | Standard Hourly remains available and is the only policy considered |

### Early Bird and Night Owl

| Scenario | Expected result |
|---|---|
| Early Bird entry at 06:00 and exit at 15:30 on an eligible weekday | Applicable; both starts are inclusive |
| Early Bird entry at 09:00, exit at 15:29:59, or exit at 19:00 | Not applicable at the exclusive/out-of-window boundary |
| Early Bird entry and exit on different dates | Not applicable |
| Early Bird on weekend or OPM federal/observed holiday | Not applicable |
| Night Owl entry at 18:00 and exit at 05:00 next date | Applicable when both dates are eligible weekdays |
| Night Owl entry before 18:00, exit before 05:00, or exit at 10:00 | Not applicable at the exclusive/out-of-window boundary |
| Night Owl entry on the last second of a calendar day and exit in its next calendar day window | Apply the specified inclusive entry-day end and next-consecutive-date rule |
| Night Owl where either entry or exit date is a weekend or OPM federal/observed holiday | Not applicable |
| Each loyalty tier on each special | Apply None=0%, Silver=10%, Gold=20%, Platinum=30% |
| Motorcycle and Bus on each special | Apply the vehicle multiplier to the discounted special rate |
| Stay longer than 24 hours | Neither special returns a candidate |

### Money and best-value selection

| Scenario | Expected result |
|---|---|
| Positive exact value at a half-cent boundary (for example $10.005) | Round half-up to $10.01 |
| Positive exact value below the half-cent boundary (for example $10.004) | Round to $10.00 |
| Candidate rates have different exact amounts | Select the least exact amount before rounding |
| Candidate amounts are exactly equal | Choose Early Bird, then Night Owl, then Standard Hourly |
| Candidate amounts differ exactly but round to equal cents | Choose the lower exact candidate; do not apply tie precedence |
| Stay is valid and all policies are evaluated | Return a quote for the lowest applicable candidate |
| A policy is inapplicable | Exclude it without preventing other policies from being evaluated |
| Standard Hourly is the only candidate | Return its rounded USD fare |

Half-up rounding tests use direct `Money` inputs because the current configured rates and multipliers may not naturally generate a fractional cent. This verifies the required money behavior without inventing a new rate.

## In-memory flow tests

1. **Successful entry and exit:** Issue a ticket with a compatible spot and default loyalty tier, calculate a valid exit fare, mark the ticket exited, and release the spot.
2. **No compatible space:** Fill or restrict inventory so no compatible spot remains; entry is rejected, no ticket is issued, and inventory remains unchanged.
3. **Failed exit validation:** Submit an invalid exit time; no fare is returned, the ticket stays active, and its spot remains reserved.
4. **Repeated exit:** Exit a ticket successfully, then attempt a second exit; reject the second attempt without a second quote or state mutation.
5. **Policy conflict:** Use a weekday Car stay entered at 06:30 and exited at 16:00 with `None` loyalty. Verify Early Bird and Standard Hourly are both considered and the lower valid fare wins.
6. **Extended stay:** Supply a stay over 24 elapsed hours; verify only Standard Hourly participates even if test doubles for both special policies could otherwise return candidates.

## Traceability to requirements

| Requirement area | Primary tests |
|---|---|
| Vehicle categories, multipliers, and spot fit | Vehicle/spot unit tests |
| Ticket issuance and loyalty default | Ticket domain tests and successful entry flow |
| Capacity and compatible allocation | Inventory unit tests and no-compatible-space flow |
| Timestamp validity and one-time exit | Stay validation and ticket lifecycle tests |
| Hour rounding, progressive rates, and peak overlap | Standard Hourly boundary suite |
| Weekdays, weekends, and federal holidays | Standard Hourly and special eligibility suites with fixed OPM fixtures |
| Early Bird/Night Owl windows and discounts | Special-rate boundary suite |
| Vehicle multipliers, best fare, and tie precedence | Policy amount and calculator tests |
| >24-hour constraint | Special-policy and extended-stay flow tests |
| Currency behavior | Exact-amount comparison and `Money` half-up tests |
| No payment behavior | Assert exit flow returns a fare quote and has no payment dependency |

## Acceptance gate

The implementation is ready for review only when all listed behavior tests pass, invalid inputs fail explicitly, tests use deterministic calendar/time data, and no test depends on an external database, API, or live holiday service. Daylight-saving ambiguous/nonexistent-time behavior and OPM schedule refresh/version policy require explicit decisions before adding corresponding edge-case tests.
