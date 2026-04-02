# Role
Act as an SDET (Software Development Engineer in Test) specialized in Spring Modulith and TDD.

# Context
You are testing the TARDIS (Time-service) module as defined in the Capability Brief. 

# Task
Generate a suite of JUnit 5 / Spring Modulith integration tests for the TARDIS feature implementation. 

# Testing Guardrails:
1. **Event Capture**: Use `PublishedEvents` to verify that ScheduledEventTriggered events are emitted at the correct logical time.
2. **Database Isolation**: Tests must run using **H2** or a clean PostgreSQL test schema.
3. **Mocking Ports**: Mock any external WorldTime APIs or Clock providers to simulate Leap Years, DST transitions, and Aging logic (e.g. 80-year lifespan simulations).
4. **Self-Awareness Scenarios**: Specifically test the "subjective feeling" logic for Person entities (YOUNG, MIDDLE_AGED, OLD).

# Output format
Provide the complete Java code for `TardisModuleTests.java`.

---
**Target Spec:**
[READ THE SPEC LOCATED AT: docs/specs/tardis-spec.md]
