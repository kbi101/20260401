# 🪐 TARDIS Operations Runbook

This document describes how to monitor and troubleshoot the **TARDIS Time Service** and its scheduling engine.

## 1. Observability

### Key Logs
Monitor the following logs for the TARDIS module:
- `com.timelord.tardis.TardisService`: For awareness logic and schedule registration.
- `com.timelord.tardis.TardisEventPollingAdapter`: For periodic schedule trigger analysis.

### Spring Application Events TO Watch
- `ScheduledEventTriggered`: Should fire within 5 seconds of the `target_time`.
- `ScheduleRequestEvent`: Inbound requests from other modules.

### Database Monitoring
- **Namespace**: `tardis_db`
- **Tables**: `tardis_schedules`, `tardis_entity_metadata`.
- **Integrity Check**: Ensure no schedules stay in `PENDING` status for more than 1 minute after their `target_time`.

---

## 2. Troubleshooting & Incident Response

### Problem: Schedules are not triggering
- **Verification**: Check if the `Spring Scheduler` is enabled in the main application.
- **Verification**: Ensure the `tardis_schedules` status is `PENDING` and not `CANCELLED`.
- **Resolution**: Toggle the `tardis.scheduler.enabled` feature flag off and on.

### Problem: Leap Year or DST Drift
- **Verification**: TARDIS uses `java.time.ZonedDateTime` and `LocalDateTime` for all calculations, which are DST-aware.
- **In-Depth Check**: If drift is suspected, use the **ClockPort** in integration tests to simulate the specific transition moment and verify the awareness logic.

---

## 3. Configuration Flags

| Setting | Context | Default |
| :--- | :--- | :--- |
| `tardis.scheduler.polling-interval` | ms | `5000` |
| `tardis.scheduler.entity-birth-default` | fallback | `1970-01-01T00:00:00` |
| `tardis.scheduler.lifespan-max` | years | `80` |
