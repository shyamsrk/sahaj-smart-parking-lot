# AI Chat History — User Prompts and Decisions

This file records the user-authored prompts and specification decisions for this project. The two source PDFs are referenced by their supplied paths rather than copied here. System/developer instructions and private internal reasoning are not chat prompts and are not reproduced.

## Initial request

> [Assignment_Instructions.pdf](file:///home/shyamsrk/Assignment_Instructions.pdf) [The_Smart_Parking_Lot_v2.pdf](file:///home/shyamsrk/The_Smart_Parking_Lot_v2.pdf) Generate a software architecture plan as an architect based on the attached specification documents. The plan should include specification, design, domain objects, and tests. While working on the plan and ask questions, do not assume anything and explicitly call out assumptions to get clarification. Go step by step instead of one-shot generation. Follow the below instructions:
>
> 1) All the chat history prompts should be saved, checked in and shared with the project in a file [ai-chat-history.md](http://ai-chat-history.md).
>
> 2). Readme should be specific to the project and should be a separate file.
>
> 3) I need to push this code into github. A new repo should be created.
>
> 4) Keep monitoring the context and credits, if the usage crosses 70% of utilization let me know.
>
> 5) Always pause and take confirmation before going to the next step of the plan.
>
> 6) Push the code to the repository after each stage.

## Source documents supplied

- User message: `Document file "Assignment_Instructions.pdf" at path "/home/shyamsrk/Assignment_Instructions.pdf"`.
- `Assignment_Instructions.pdf`, supplied at `/home/shyamsrk/Assignment_Instructions.pdf`.
- User message: `Document file "The_Smart_Parking_Lot_v2.pdf" at path "/home/shyamsrk/The_Smart_Parking_Lot_v2.pdf"`.
- `The_Smart_Parking_Lot_v2.pdf`, supplied at `/home/shyamsrk/The_Smart_Parking_Lot_v2.pdf`.

The assignment instructions require maintainable design, domain modeling, tests/validation, standard-library/platform-agnostic technology, README assumptions and design decisions, and transparent AI prompt history. The parking-lot specification describes the vehicle/spot domain, vehicle multipliers, Standard Hourly, Early Bird and Night Owl rates, best-value selection, and the >24-hour rule.

## User's initial clarification answers

The user supplied these answers to the clarification form:

- Public holidays: “Consider US Federal holidays as the holidays. For hourly pricing the base hourly rate apply every day, with only peak surcharge limited to weekdays.”
- Loyalty and multipliers: “Default the loyalty tier as None. Yes.”
- Special-rate boundaries / >24-hour rule: “yes.”
- Invalid inputs and precision: “Treat them as invalid inputs and raise an appropriate exception. Fractional cents should be rounded half-up,”
- Tie-break and scope: “In case of tie break use Early Bird, Nightly Biird, Regular Hour. The architecture should cover rate-caluculation engine, ticket Issuance and spot assiganments based on the compatibility of the vehicle. Payment is a future requirement that we can ignore for now.”
- Repository: “sahaj-smart-parking-lot, The owner is shyam srk and public visibility.”

## Follow-up clarification answers

The user supplied these answers to the follow-up clarification form:

- No loyalty tier: “Early Bird/Night Owl remain eligible”
- Holiday/date timezone: “Facility timezone is required configuration”
- Spot compatibility: “Exactly these combinations”
- Rounding: “Final fare only”
- Policy boundaries: “Yes, use those rules”
- GitHub owner login: “defer”

## Stage 1 review and final clarifications

- Stage 1 review: “Accept Stage 1 as written”
- Early Bird / Night Owl days: “Weekdays excluding federal holidays”
- Federal holiday convention: “Official federal holiday dates and observed weekdays”
- Loyalty tier input: “Stored on ticket”
- GitHub owner login: “shyamsrk”

## Stage 2 authorization

> Yes, proceed to Stage 2

## Stage 2 review and clarifications

- Stage 2 review: “Accept Stage 2 as written”
- Fare tie basis: “Compare exact amounts; tie only if exact values equal”
- Night Owl weekday check: “Both entry and exit dates”
- Holiday source: “Use the US OPM federal holiday schedule”
- Timezone/DST: “Default to US/Los Angeles for the current implementation”
- Ticket and spot lifecycle: “The ticket should be exited once only and the spot should be realeased on a successful exit. If no compatible spot is available show an appropriate message and do not allow entry.”
- Process restart behavior: “In-memory state is acceptable”

## Stage 3 authorization

> Yes, proceed to Stage 3

## Stage 3 review

> Accept Stage 3 as written

## Technology-stack request and availability check

> Looks fine. Lets move to the next stage. Use java17, Maven and Junit as the technology stock for the implementation. First confirm the tech stack availability and pause before proceeding.

Availability check: OpenJDK 17.0.21.1 and Maven 3.9.9 are installed; JUnit Jupiter 5.11.4 artifacts are cached locally. No `pom.xml` existed. No project files were changed at this check.

> No, pause here

## System architecture request

> Generate a system/archtiectural design and pause for the next instruction

## Architecture clarification answers

> Lets resolve any open questions

- Holiday data: “Versioned data resource in the repository, updated from OPM”
- Timestamp/DST: “Instant-only; elapsed duration uses the instant timeline”
- Concurrency: “Allow concurrent calls; serialize compound in-memory state transitions”
- Spot order: “Lowest spot ID first”
- Java base package: `com.sahaj.smartparkinglot`
- Build settings: “JUnit Jupiter 5.11.4; include Maven Wrapper 3.9.9”
- Ticket IDs: “JDK UUID”
- Error contract: “Typed domain/application exceptions; caller formats messages”

## Implementation pause

> pause before going in to implementation

## Architecture diagram request

> Lets create system/archtiectural design daigrams as images

## Implementation phase request

> Let's generate the code now. Follow the instructions
> 1. Do not go for a one shot generation. Approach phase by phase.
> 2. Generate the code phases in the order Domain objects, Interfaces, and then the Business logic.
> 3. Write the required unit tests for each phase and push the code when all the tests are passing.
> 4. Pause and confirm for the next stage.

## Recorded decisions

- Repository should be named `shyamsrk/sahaj-smart-parking-lot` and public. The exact owner login was later confirmed as `shyamsrk`.
- A stage must be reviewed and confirmed before work proceeds to the next stage.
- Push after each stage. The GitHub CLI was authenticated as `shyamsrk`; the requested repository did not exist when checked and was created for the Stage 1 push.
- Stage 1 covers the business specification. Stage 2 covers architecture and domain model, including confirmed one-time exit, spot release, no-space rejection, in-memory state, OPM holiday calendar, Los Angeles timezone default, exact tie comparison, and both-date Night Owl eligibility. Stage 3 documents test strategy and scenarios and was accepted as written.
- Java 17, Maven Wrapper 3.9.9, and JUnit Jupiter 5.11.4 are the selected implementation stack. Architecture decisions include versioned OPM resource data, Instant-only time inputs, synchronized concurrent state transitions, lowest-ID spot assignment, `com.sahaj.smartparkinglot`, JDK UUIDs, and typed exceptions. Implementation has not started.
