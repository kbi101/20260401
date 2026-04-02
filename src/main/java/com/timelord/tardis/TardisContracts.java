package com.timelord.tardis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public enum EntityType {
    PERSON, ANIMAL, PLANT, TASK, EMAIL_CHAIN, SYSTEM
}

public enum ConfidenceLevel {
    HIGHEST, PROBABLE, ESTIMATED, SPECULATIVE
}

public enum LifeStage {
    YOUNG, MIDDLE_AGED, OLD, UNKNOWN
}

public record EntityTemporalContext(
    String entityId,
    EntityType type,
    String timezone,
    LocalDateTime birthTimestamp,
    ConfidenceLevel sourceConfidence
) {}

public record EntitySelfAwareness(
    String entityId,
    String relativeAge,
    LifeStage lifeStage,
    boolean isDaylight,
    LocalDateTime nextTransition,
    ConfidenceLevel currentConfidence
) {}

public record EmailChainMetadata(
    String chainId,
    List<LocalDateTime> timelinePoints,
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

// Events
record ScheduleRequestEvent(FutureSchedule schedule) {}
record ScheduledEventTriggered(String scheduleId, String ownerModule, String payloadJson) {}
record ReminderDueEvent(String entityId, String message) {}
