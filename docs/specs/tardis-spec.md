# Capability Brief: TARDIS (Time and Relative Dimension in Space) Service

This specification defines the architectural requirements for the `tardis` module. The TARDIS service provides centralized time-related intelligence, scheduling, and temporal arithmetic to other modules and external agents within the Timelord ecosystem.

## 1. Feature Intent (Job-to-be-Done)
- **Goal:** To provide a robust, high-precision temporal engine that abstracts complex time calculations and manages the temporal self-awareness of entities (Living Things, Tasks, Email Chains) through context-aware aging, scheduling, and environmental cycles.
- **Protocol:** REST API for synchronous arithmetic; Spring Application Events for asynchronous schedule triggers.
- **Entity Intelligence:** 
    - **Living Things (Person/Animal/Plant):** Real-time age and life-cycle relative time based on birth/origin.
    - **Circadian Context:** Reporting Day/Night status based on timezone-aware environmental cycles for Earth-based entities.
    - **Tasks:** One-time and periodic execution tracking and next-trigger forecasting.
    - **Email Chains:** Deep temporal analysis of message chains (sent, received, reply, and nested reply-to-reply patterns).
- **Inaccuracy Handling:** All temporal results MUST include or honor a **ConfidenceLevel** (e.g., HIGHEST, PROBABLE, ESTIMATED) when data is incomplete.
- **Architecture:** 
    - **Temporal Engine (Stateless):** Pure logic for conversions, arithmetic, and entity self-awareness logic.
    - **Persistence Layer (Stateful):** Storage for scheduled events, entity origin timestamps, and reminders.
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
public enum EntityType {
    PERSON, ANIMAL, PLANT, TASK, EMAIL_CHAIN, SYSTEM
}

public enum ConfidenceLevel {
    HIGHEST, PROBABLE, ESTIMATED, SPECULATIVE
}

public record EntityTemporalContext(
    String entityId,
    EntityType type,
    String timezone,      // Source of truth for Circadian context
    LocalDateTime birthTimestamp, // or task creation / email genesis
    ConfidenceLevel sourceConfidence
) {}

public record EntitySelfAwareness(
    String entityId,
    String relativeAge,   // e.g. "25 years", "2 days ago (received)"
    boolean isDaylight,   // True if sun has risen in target timezone
    LocalDateTime nextTransition, // next sunrise/sunset or next task run
    ConfidenceLevel currentConfidence
) {}

public record EmailChainMetadata(
    String chainId,
    List<LocalDateTime> timelinePoints, // sent, received, reply, nested reply
    Duration averageResponseTime,
    ConfidenceLevel timelineConfidence
) {}

public record FutureSchedule(
    String scheduleId,
    String ownerModule,
    LocalDateTime targetTime,
    boolean isPeriodic,
    String cronExpression,
    String payloadJson
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
| `/api/v1/tardis/awareness` | `POST` | Get self-awareness/aging/circadian info for an entity. | `EntityTemporalContext` |
| `/api/v1/tardis/emails/{chainId}` | `GET` | Perform deep temporal analysis of an email chain. | `EmailChainMetadata` |
| `/api/v1/tardis/schedules` | `POST` | Register a new future event trigger (One-time/Periodic). | `FutureSchedule` |
| `/api/v1/tardis/schedules/{id}` | `DELETE` | Cancel a pending schedule. | - |

## 7. Evaluation Metrics (Acceptance Criteria)
- **Accuracy:** 100% precision on Leap Year and Daylight Saving Time (DST) transitions.
- **Throughput:** Capable of handling 1000+ per-second arithmetic requests.
- **Latency:** Sub-10ms response time for stateless arithmetic operations.

---
