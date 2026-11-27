# Stage 1: Business Specification

## Purpose and scope

Specify the behavior of a smart parking lot's ticket, spot-assignment, and rate-calculation capabilities. On exit, the system evaluates every valid rate policy, applies the vehicle multiplier and any loyalty discount, and charges the lowest valid fare.

### In scope

- Issue a ticket recording a vehicle's entry timestamp and vehicle classification.
- Assign a spot compatible with the vehicle.
- Calculate a final USD parking fee at exit using all applicable pricing policies.
- Reject invalid input with an appropriate exception.

### Out of scope

- Payment collection, payment settlement, or refunds.
- Database, web API, GUI, or a specific deployment topology.
- A selected programming language or executable command interface; these belong to later design decisions and must respect the assignment's standard-library/no-heavy-framework constraint.

## Domain facts

| Vehicle | Base-rate multiplier | Compatible spot |
|---|---:|---|
| Motorcycle | 0.8 | Compact |
| Car | 1.0 | Compact |
| Bus | 2.0 | Large |

All listed rates are USD. Currency conversion is not needed.

Tickets are issued on entry and retain a precise entry timestamp. An exit timestamp is supplied when calculating the fee. The facility's timezone is needed to evaluate local dates and policy time windows; for the current implementation it defaults to `America/Los_Angeles`.

## Pricing requirements

All policy rates below are defined relative to the Car rate. Apply the relevant vehicle multiplier to the selected policy's amount. For Early Bird and Night Owl, apply the loyalty discount to the base special rate, then apply the vehicle multiplier. Round the selected final amount once to cents using half-up rounding.

### Standard Hourly

- Evaluate this policy for every valid stay, including stays longer than 24 hours.
- Round elapsed duration upward to a whole number of hourly segments. A stay with any positive partial hour incurs that hour; a zero or negative duration is invalid.
- Segments are floating blocks: segment 1 begins at entry, and each next segment begins where the previous segment ends.
- Car base rate: $5.00 for segment 1, $3.00 for segment 2, and $2.00 for each later segment.
- Apply a 1.5 multiplier to an entire segment if any part of it intersects either local-time peak window: 07:00–10:00 or 16:00–19:00.
- Peak-window starts are inclusive and ends exclusive. An overlap with even part of a segment makes that whole segment a peak segment.
- Peak surcharge applies only Monday through Friday, excluding US federal holidays. Hourly base pricing still applies on weekends and holidays.

### Early Bird

- Car base rate: $15.00.
- Entry local time is in [06:00, 09:00); exit local time is in [15:30, 19:00) on the same calendar date.
- Apply the loyalty discount: Silver 10%, Gold 20%, Platinum 30%, and None 0%.
- Apply the vehicle multiplier to the discounted special rate.
- This special is eligible only Monday through Friday, excluding US federal holidays.

### Night Owl

- Car base rate: $8.00.
- Entry local time is in [18:00, end of calendar day]; exit local time is in [05:00, 10:00) on the next consecutive calendar date.
- Apply the same loyalty discount percentages as Early Bird, with None receiving 0%.
- Apply the vehicle multiplier to the discounted special rate.
- This special is eligible only Monday through Friday, excluding US federal holidays.

### Best-value selection and extended stays

- Evaluate each policy whose applicability conditions are satisfied; do not stop after finding the first valid policy.
- Select the valid policy yielding the minimum final fare.
- For equal fares, use this precedence: Early Bird, Night Owl, Standard Hourly.
- For a stay longer than 24 hours, Early Bird and Night Owl are invalid and Standard Hourly is the only evaluated policy.

## Input validation

Inputs that are invalid—including an exit time that is not after entry, missing required ticket/session data, or unsupported vehicle classification—must fail explicitly with an appropriate exception. The exact exception taxonomy is an implementation decision. The customer's loyalty tier is stored on the ticket and defaults to None.

## Confirmed clarifications

These are decisions explicitly supplied by the user during specification clarification:

1. Use the US Office of Personnel Management (OPM) federal holiday schedule, including observed weekdays, when determining weekdays. Hourly base pricing applies every day; the peak surcharge and both special rates are limited to weekdays excluding federal holidays.
2. Default the facility timezone to `America/Los_Angeles` for the current implementation.
3. Use exactly the three listed vehicle/spot compatibility pairs: Motorcycle→Compact, Car→Compact, Bus→Large.
4. A loyalty tier of None leaves Early Bird and Night Owl eligible with no discount.
5. Apply vehicle multipliers to special rates.
6. Round half-up only after selecting the final fare, not per hourly segment.
7. Tie precedence is Early Bird → Night Owl → Standard Hourly.
8. A stay exceeding 24 hours invalidates both specials and uses Standard Hourly only.
9. Raise an appropriate exception for invalid inputs.
10. Include ticket issuance, spot compatibility/assignment, and rate calculation; exclude payment.
11. A customer's loyalty tier is stored on the ticket and defaults to None.
12. Night Owl requires both its entry and exit dates to be weekdays excluding federal holidays.
13. Determine fare ties from exact amounts, not from amounts that only become equal after cent rounding.
14. A ticket may exit once only; release its spot after successful exit.
15. If no compatible spot is available, reject entry with an appropriate message.
16. Keep ticket and spot state in memory; survival across process restarts is not required.

## Technical clarifications

- Supply the OPM federal/observed holiday schedule through versioned per-year repository resources; update from OPM without runtime network calls.
- Accept entry and exit as `Instant` values only. Elapsed duration follows the instant timeline; convert to `America/Los_Angeles` local time for policy windows.
- Permit concurrent entry/exit calls and serialize state transitions in the in-memory facility aggregate.
- Assign the lowest compatible spot ID first using ascending natural `String` order over canonical IDs.
- Use JDK UUID ticket IDs and typed domain/application exceptions. The caller formats error messages.

These clarify implementation boundaries without changing the pricing rules.

## Acceptance criteria for this specification

- The three vehicle types, multipliers, and spot-compatibility pairs are represented exactly as stated.
- Hourly pricing rounds duration upward, uses floating hourly blocks, evaluates partial peak overlap, and observes inclusive-start/exclusive-end peak windows.
- Peak surcharges do not apply on weekends or US federal holidays; the hourly base rate remains applicable.
- Special rates use their stated local-time/date windows, discounts, and vehicle multipliers.
- Early Bird and Night Owl are eligible only on weekdays excluding OPM federal holidays (including observed weekdays); Night Owl checks both entry and exit dates.
- Valid policies are compared exhaustively and the lowest fare is selected with the specified tie precedence.
- Stays exceeding 24 hours are billed using Standard Hourly alone.
- Invalid input fails explicitly, and final USD fares use half-up cent rounding.
