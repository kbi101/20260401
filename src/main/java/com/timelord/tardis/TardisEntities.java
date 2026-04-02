package com.timelord.tardis;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tardis_schedules", schema = "tardis_db")
class ScheduledEventEntity {
    @Id
    private String scheduleId;
    private String ownerModule;
    private LocalDateTime targetTime;
    private boolean isPeriodic;
    private String cronExpression;
    
    @Column(columnDefinition = "TEXT")
    private String payloadJson;
    
    private String status; // PENDING, TRIGGERED, CANCELLED

    protected ScheduledEventEntity() {}

    public static ScheduledEventEntity fromRecord(FutureSchedule record) {
        ScheduledEventEntity entity = new ScheduledEventEntity();
        entity.scheduleId = record.scheduleId();
        entity.ownerModule = record.ownerModule();
        entity.targetTime = record.targetTime();
        entity.isPeriodic = record.isPeriodic();
        entity.cronExpression = record.cronExpression();
        entity.payloadJson = record.payloadJson();
        entity.status = "PENDING";
        return entity;
    }

    public String getScheduleId() { return scheduleId; }
    public LocalDateTime getTargetTime() { return targetTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOwnerModule() { return ownerModule; }
    public String getPayloadJson() { return payloadJson; }
}

@Entity
@Table(name = "tardis_entity_metadata", schema = "tardis_db")
class EntityMetadataEntity {
    @Id
    private String entityId;
    
    @Enumerated(EnumType.STRING)
    private EntityType type;
    
    private String timezone;
    private LocalDateTime birthTimestamp;
    
    @Enumerated(EnumType.STRING)
    private ConfidenceLevel confidence;

    protected EntityMetadataEntity() {}
    
    public String getEntityId() { return entityId; }
    public EntityType getType() { return type; }
    public String getTimezone() { return timezone; }
    public LocalDateTime getBirthTimestamp() { return birthTimestamp; }
    public ConfidenceLevel getConfidence() { return confidence; }
}
