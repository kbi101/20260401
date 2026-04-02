# Capability Brief: TARDIS (Time and Relative Dimension in Space) Service

This specification defines the architectural requirements for the `tardis` module. The TARDIS service provides centralized time-related intelligence, scheduling, and temporal arithmetic to other modules and external agents within the Timelord ecosystem.

## 1. Feature Intent (Job-to-be-Done)
- **Goal:** To provide a robust, high-precision temporal engine that abstracts complex time calculations (Timezones, Business Days, Relative offsets) and manages scheduled reminders/events for other system components.
- **Protocol:** REST API for synchronous arithmetic; Spring Application Events for asynchronous schedule triggers.
- **Architecture:** 
    - **Temporal Engine (Stateless):** Pure logic for conversions and arithmetic.
    - **Persistence Layer (Stateful):** Storage for scheduled events and reminders in the `tardis_schedules` table.
- **Database Support:** 
    - **Production:** PostgreSQL 17+ (schema: `tardis_db`).
    - **Development:** H2 / SQLite.

## 2. Key Capabilities
- **Timezone Intelligence:** Precision conversion between any IANA timezone identifiers.
- **Temporal Arithmetic:** Support for "Relative Time" parsing (e.g., "3 business days from now", "next Tuesday at 4pm").
- **Business Day Awareness:** Calculation of deadlines excluding weekends and optionally bank holidays (via external provider).
- **Scheduling Core:** Ability for other modules to "Register" a callback or event to be fired at a specific future timestamp.
- **Human-Agent Formatting:** Generating human-readable relative strings (e.g., "in 2 hours", "yesterday") for AI agent UI/UX.

## 3. Event Contracts (Spring Application Events)
- **Inbound Events:**
    - `ScheduleRequestEvent`: Request from another module to trigger a future action.
    - `CancelScheduleEvent`: Request to abort a pending scheduled event.
- **Outbound Events:**
    - `ScheduledEventTriggered`: Emitted when a registered time-threshold is reached.
    - `ReminderDueEvent`: Specific subset of triggers for user-facing notifications.

## 4. Strict Data Schemas (Java Records)

### Input/Context Schemas
```java
public record TimeConversionRequest(
    String inputTimestamp, // ISO-8601
    String sourceZone,     // e.g., "UTC"
    String targetZone      // e.g., "America/New_York"
) {}

public record RelativeTimeRequest(
    String anchorTimestamp,
    String expression,     // e.g., "plus 3 business days"
    boolean includeHolidays
) {}

public record FutureSchedule(
    String scheduleId,
    String ownerModule,    // e.g., "inbox-intelligence"
    LocalDateTime targetTime,
    String payloadJson     // Context to be returned upon trigger
) {}
```

## 5. Architecture & Guardrails
- **Precision:** All internal storage MUST be in **UTC**. Conversions to local time MUST occur only at the edge (API/UI).
- **Resilience:** The scheduling engine MUST resume pending triggers after an application restart (Persistent Queue).
- **Idempotency:** Re-registering a schedule with the same `scheduleId` MUST perform an update rather than a duplicate entry.
- **Clock Drift:** The system MUST allow for a configurable "Clock Offset" during testing/simulation to speed up or slow down perceived time.

## 6. REST API Interface

| Endpoint | Method | Purpose | Payload |
| :--- | :--- | :--- | :--- |
| `/api/v1/tardis/convert` | `POST` | Convert timestamps between zones. | `TimeConversionRequest` |
| `/api/v1/tardis/arithmetic` | `POST` | Calculate future/past dates based on expressions. | `RelativeTimeRequest` |
| `/api/v1/tardis/schedules` | `POST` | Register a new future event trigger. | `FutureSchedule` |
| `/api/v1/tardis/schedules/{id}` | `DELETE` | Cancel a pending schedule. | - |

## 7. Evaluation Metrics (Acceptance Criteria)
- **Accuracy:** 100% precision on Leap Year and Daylight Saving Time (DST) transitions.
- **Throughput:** Capable of handling 1000+ per-second arithmetic requests.
- **Latency:** Sub-10ms response time for stateless arithmetic operations.

---
