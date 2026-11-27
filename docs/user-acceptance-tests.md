# Smart Parking Lot User Acceptance Tests

## Purpose and scope

This document defines acceptance scenarios for the initial interactive Smart Parking Lot application and records verification performed to date. It covers entry, spot allocation, parking status, exit and fare calculation, loyalty discounts, Early Bird and Night Owl eligibility, boundary conditions, and negative scenarios.

Payment collection, persistence across application restarts, multiple facilities, and network APIs are not part of this release.

## Acceptance environment

- Java 17 and the repository Maven Wrapper.
- Start the CLI from the repository root:

  ```sh
  ./mvnw compile
  java -cp target/classes com.sahaj.smartparkinglot.Main
  ```

- The menu supports entry, exit by ticket ID, parking status, and quit.
- The default facility contains 10 compact spots (`C-01` through `C-10`) and 5 large spots (`L-01` through `L-05`).
- Enter entry and exit values as ISO-8601 UTC instants, such as `2026-01-05T16:00:00Z`. Policy windows are evaluated in `America/Los_Angeles`.
- Bundled US federal holiday data is available for 2025-2029. A pricing request requiring unavailable calendar data must fail explicitly.
- Ticket and occupancy state lasts only for the current process. Copy the issued ticket ID to use it for exit.
- All fares are USD. The CLI displays the lowest eligible quote; it does not collect payment.

## UAT execution status

`PASS` means a scripted CLI scenario was executed successfully. `AUTOMATED PASS` means the behavior was verified by an automated unit/service test rather than through the CLI. `PENDING` means it has not been individually executed. Manual stakeholder UAT sign-off remains separate from these results.

### Results captured

| ID | Executed check | Result | Evidence |
|---|---|---|---|
| RES-01 | Full Maven test suite | **PASS** — 73 tests, 0 failures, 0 errors, 0 skipped | `./mvnw test`, run 2026-11-27 |
| RES-02 | CLI: enter a Car, show status, quit | **PASS** — ticket issued at `C-01`; status showed one active ticket, 9/10 compact spots and 5/5 large spots available | Scripted CLI smoke test, run 2026-11-27 |
| RES-03 | CLI: submit unsupported vehicle `TRUCK` | **PASS** — clear validation error shown; menu remained usable and accepted Quit | Scripted CLI smoke test, run 2026-11-27 |
| RES-04 | Menu-driven entry, status, exit, status, quit | **PASS** — fare returned, active ticket removed from status, spot became available | `MainTest` automated flow |
| RES-05 | Invalid vehicle and repeated exit handling | **PASS** — input error and already-exited error handled without terminating the menu | `MainTest` automated cases |
| RES-06 | Scripted CLI acceptance run | **PASS** — 93 assertions covering operations, capacity, all Early Bird/Night Owl vehicle-tier combinations, policy boundaries, hourly pricing, DST, and negative/recovery paths | Interactive CLI run, 2026-11-27 |
| RES-07 | Exact quote selection, money rounding, and concurrent entry/exit | **AUTOMATED PASS** — exact-tie precedence, exact-amount comparison before rounding, half-up cent rounding, and single-winner concurrent operations verified | `RateCalculatorTest`, `MoneyTest`, `InMemoryParkingFacilityTest`; `./mvnw test`, 73 tests passed, 0 failures |

The scenario statuses below record direct scripted execution or automated test evidence. Manual stakeholder UAT sign-off is still pending. Record tester, execution date, actual result, and evidence for any additional manual acceptance.
Scenarios that require controlled races or arbitrary exact-money inputs must be run through a service/domain test harness; the interactive CLI does not expose those controls.

## Test data and fare reference

### Vehicle and spot compatibility

| Vehicle | Required spot | Fare multiplier |
|---|---|---:|
| Motorcycle | Compact | 0.8 |
| Car | Compact | 1.0 |
| Bus | Large | 2.0 |

When multiple compatible spots are free, the lowest canonical spot ID is assigned (for example, `C-01` before `C-02`). A blank loyalty input defaults to `NONE`.

### Special-rate candidate amounts

The following are exact candidate amounts for each special policy, after applying the loyalty discount and vehicle multiplier. The final quote is still the lowest amount among all policies eligible for that stay.

#### Early Bird — Car base rate $15.00

| Loyalty tier | Discount | Motorcycle | Car | Bus |
|---|---:|---:|---:|---:|
| NONE | 0% | $12.00 | $15.00 | $30.00 |
| SILVER | 10% | $10.80 | $13.50 | $27.00 |
| GOLD | 20% | $9.60 | $12.00 | $24.00 |
| PLATINUM | 30% | $8.40 | $10.50 | $21.00 |

#### Night Owl — Car base rate $8.00

| Loyalty tier | Discount | Motorcycle | Car | Bus |
|---|---:|---:|---:|---:|
| NONE | 0% | $6.40 | $8.00 | $16.00 |
| SILVER | 10% | $5.76 | $7.20 | $14.40 |
| GOLD | 20% | $5.12 | $6.40 | $12.80 |
| PLATINUM | 30% | $4.48 | $5.60 | $11.20 |

## Acceptance scenarios

Policy-window times in these scenarios are facility-local `America/Los_Angeles` times. Convert them to UTC instants for CLI input; account for PST/PDT when choosing dates.

### A. Menu and parking operations

| ID | Scenario and steps | Expected result | Status |
|---|---|---|---|
| UAT-001 | Start the application; select Quit. | Menu appears, application exits cleanly. | PASS |
| UAT-002 | Enter a Car, Motorcycle, and Bus in separate transactions. | Car/Motorcycle receive compact spots; Bus receives a large spot. Each entry prints a unique ticket ID and assigned spot. | PASS |
| UAT-003 | Enter multiple vehicles before exiting any; then show status. | All active tickets appear, sorted by spot ID; availability reflects occupied spots. | PASS |
| UAT-004 | Exit an active ticket using its ID and a later exit instant; show status again. | Fare and policy are printed; ticket is no longer active; its spot is available again. | PASS |
| UAT-005 | Exit one ticket while other tickets remain active. | Only the selected ticket exits; remaining tickets and occupancy remain unchanged. | PASS |
| UAT-006 | Fill all 10 compact spots, then attempt one more Car or Motorcycle entry. Repeat for all 5 large spots and a Bus. | Extra entry is rejected with a no-compatible-space message; no ticket is issued and occupied spots are unchanged. | PASS |
| UAT-007 | Enter an eligible vehicle while several compatible spaces are free. | Lowest compatible spot ID is assigned. | PASS |
| UAT-008 | Enter a vehicle without a loyalty value (blank line); inspect or exit its ticket. | Ticket uses `NONE`; no loyalty discount is applied. | PASS |

### B. Loyalty, Early Bird, and Night Owl

Use an eligible weekday that is not an OPM federal/observed holiday. Verify each tier against the candidate tables above. For each tier, submit an Early Bird stay and a Night Owl stay; confirm the selected final policy and amount after comparing all eligible policies.

| ID | Scenario and steps | Expected result | Status |
|---|---|---|---|
| UAT-010 | Early Bird: same local date, entry at 06:00, exit at 15:30. Repeat with `NONE`, `SILVER`, `GOLD`, and `PLATINUM` for each vehicle type. | Special is eligible at inclusive starts; candidate equals the Early Bird table. Apply the vehicle multiplier after discount. | PASS |
| UAT-011 | Early Bird entry at 08:59:59, then at 09:00; exit at 15:29:59, then 15:30; also test 18:59:59 and 19:00. | Entry 08:59:59 and exit 15:30/18:59:59 may qualify. Entry 09:00, exit 15:29:59, and exit 19:00 do not qualify. | PASS |
| UAT-012 | Early Bird entry and exit on different local dates, on a weekend, and on an OPM holiday/observed holiday. | Early Bird is not offered; other eligible policies are still evaluated. | PASS |
| UAT-013 | Night Owl: eligible weekday entry at 18:00; exit at 05:00 on the next consecutive eligible weekday. Repeat for each loyalty tier and vehicle type. | Special is eligible at inclusive starts; candidate equals the Night Owl table. | PASS |
| UAT-014 | Night Owl entry at 17:59:59 and 18:00; next-day exit at 04:59:59, 05:00, 09:59:59, and 10:00. | 18:00 and 05:00 are inclusive; entry before 18:00 and exit at 10:00 are ineligible. Exit after 05:00 but before 10:00 qualifies if all other rules pass. | PASS |
| UAT-015 | Night Owl entry at the last instant of a weekday and exit in the next date's valid window; then test a non-consecutive date. | Next-date window qualifies only for the next consecutive calendar date; non-consecutive dates do not qualify. | PASS |
| UAT-016 | Night Owl where entry or exit date is a weekend or federal/observed holiday. | Night Owl is ineligible if either date is not an eligible weekday. | PASS |
| UAT-017 | Keep an otherwise eligible special-rate stay longer than 24 elapsed hours. | Early Bird and Night Owl are excluded; Standard Hourly remains available. | PASS |

### C. Standard Hourly and quote selection

| ID | Scenario and steps | Expected result | Status |
|---|---|---|---|
| UAT-020 | Car stay of exactly 1 hour, then 1 hour plus a positive remainder, exactly 2 hours, and 3 hours outside peak windows. | Car totals are $5.00, $8.00, $8.00, and $10.00 respectively before any peak multipliers; partial hours round up by segment. | PASS |
| UAT-021 | Test an hourly segment with any overlap in 07:00-10:00 or 16:00-19:00 on an eligible weekday. | Entire segment's rate receives the 1.5 peak multiplier. | PASS |
| UAT-022 | Test segments ending exactly at 07:00, starting at 07:00/16:00, starting at 10:00/19:00, and overlapping just before 10:00/19:00. | Peak starts are inclusive; peak ends are exclusive; any overlap before the end applies the multiplier. | PASS |
| UAT-023 | Repeat a peak-window stay on a weekend and on a federal/observed holiday. | Base hourly pricing still applies, but peak surcharge does not. | PASS |
| UAT-024 | Compare an eligible Early Bird or Night Owl quote with Standard Hourly. Include a tie and amounts that differ before rounding but become equal after rounding. | Lowest exact amount wins; exact ties use Early Bird, then Night Owl, then Standard Hourly. A rounded-cent tie is not treated as an exact tie. | AUTOMATED PASS |
| UAT-025 | Test Motorcycle and Bus Standard Hourly stays. | Apply 0.8 and 2.0 multipliers respectively. | PASS |
| UAT-026 | Test stays across midnight and across spring-forward/fall-back using distinct instants. | Local peak/eligibility dates are evaluated correctly; duration is based on elapsed instants, not wall-clock labels. | PASS |
| UAT-027 | At the money/rate-calculator test boundary, round exact amounts $10.005 and $10.004. | Half-up rounding returns $10.01 and $10.00 respectively; rounding occurs after selecting the winning exact quote. | AUTOMATED PASS |
| UAT-028 | Using a service-level concurrency harness, race two entry requests for one compatible spot, then race two exits for one active ticket. | Exactly one entry reserves the spot; exactly one exit produces a quote and releases it. | AUTOMATED PASS |

### D. Negative and recovery scenarios

| ID | Scenario and steps | Expected result | Status |
|---|---|---|---|
| UAT-030 | Select an unknown menu option; enter an unsupported vehicle or loyalty tier. | Clear validation message; no ticket is issued; menu remains usable. | PASS |
| UAT-031 | Enter malformed entry/exit instants or malformed ticket ID. | Clear input error; no fare is returned and no invalid state transition occurs. | PASS |
| UAT-032 | Attempt exit at the entry instant or before it. | Exit is rejected; ticket remains active and its spot remains occupied; user can retry with a valid exit instant. | PASS |
| UAT-033 | Exit using an unknown UUID, then an already-exited ticket ID. | Unknown ticket and duplicate exit are rejected explicitly; no second fare is produced. | PASS |
| UAT-034 | Request pricing for dates without bundled holiday resources (for example, 2030). | Pricing fails explicitly with a holiday-data/configuration message; no successful quote is shown. | PASS |
| UAT-035 | Attempt to enter after compatible inventory is full. | No-space error is shown; no ticket or occupancy change occurs; status remains consistent. | PASS |
| UAT-036 | Cause fare calculation to fail due to unavailable/invalid calendar configuration, then show status. | No fare is returned; the ticket remains active and its spot remains occupied. | PASS |

## Automation traceability

The passing automated suite provides regression evidence for these acceptance areas:

| Area | Automated coverage |
|---|---|
| CLI entry, status, exit, quit, invalid vehicle, repeated exit | `MainTest` |
| Ticket issuance, one-time exit, release, concurrency, no-space failures | `InMemoryParkingFacilityTest` |
| Inventory compatibility and canonical spot selection | `InMemorySpotInventoryTest` |
| Early Bird and Night Owl boundaries, loyalty discounts, and vehicle multipliers | `EarlyBirdPolicyTest`, `NightOwlPolicyTest` |
| Standard Hourly segmentation, peak windows, and date boundaries | `StandardHourlyPolicyTest` |
| Fare selection and tie handling | `RateCalculatorTest` |
| OPM holiday resource loading and invalid/missing-resource failures | `OpmFederalHolidayCalendarTest` |
| Money rounding and exact comparison | `MoneyTest`, `FareQuoteTest` |

## Sign-off record

| Tester | Date | Overall result | Notes / issue references |
|---|---|---|---|
| Automated CLI harness and Maven tests | 2026-11-27 | **Scripted and automated checks pass; manual sign-off pending** | 93 CLI assertions passed; full suite: 73 tests, 0 failures, 0 errors, 0 skipped. |
