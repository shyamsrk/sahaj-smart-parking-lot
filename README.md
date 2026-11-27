# Smart Parking Lot Rate Calculator

This repository is being developed as a design-first plan for a smart, multi-level parking lot. Its intended scope is ticket issuance, assigning a compatible parking spot, and calculating the lowest valid parking fee when a vehicle exits. Payment processing is explicitly out of scope.

The plan follows the supplied assignment constraints: keep the design simple, use platform-agnostic language and standard libraries, separate domain behavior from state, and define meaningful validation and tests. No implementation language or executable interface has been selected yet.

## Project status

Stage 1 documents the clarified business specification. The architecture, domain model, and test plan will be prepared as later stages and will not be treated as approved until reviewed.

## Confirmed decisions

- Vehicle categories are Motorcycle, Car, and Bus, with car-rate multipliers of 0.8, 1.0, and 2.0 respectively.
- The only spot compatibility pairs are Motorcycle→Compact, Car→Compact, and Bus→Large.
- Hourly base pricing applies every day. The peak surcharge is limited to weekdays, excluding US federal holidays.
- Early Bird and Night Owl are eligible only on weekdays excluding US federal holidays.
- Use official US federal holiday dates and observed weekdays.
- Early Bird and Night Owl remain eligible when loyalty is `None`; in that case, the discount is 0%.
- Store the customer's loyalty tier on the ticket; default it to `None`.
- Vehicle multipliers apply to special-rate fees as well as hourly fees.
- Invalid inputs should raise an appropriate exception.
- Calculate the selected fare and round the final amount to cents using half-up rounding.
- If fares tie, prefer Early Bird, then Night Owl, then Standard Hourly.
- If a stay exceeds 24 hours, only Standard Hourly is evaluated.
- Facility timezone is required configuration. Payment is not part of this plan.

## Source documents

- `Assignment_Instructions.pdf` — assignment design, testing, technology, and AI-transparency requirements supplied by the user.
- `The_Smart_Parking_Lot_v2.pdf` — parking and pricing requirements supplied by the user.

The source PDFs were provided from `/home/shyamsrk/` and are not copied into this repository.

## Open specification questions

The specification stage records remaining questions rather than silently selecting behavior:

- What exact source/version of the official US federal holiday calendar should be used?
- Which locale/timezone database and daylight-saving transition behavior are required for facility-local timestamps?
- Should ticket exits/billing be single-use, and what should happen when no compatible spot is available?

See [`docs/specification.md`](docs/specification.md) for the full functional specification and acceptance criteria, and [`ai-chat-history.md`](ai-chat-history.md) for the user prompts and decisions recorded for this project.
