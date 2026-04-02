package com.timelord.tardis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
public class TardisService {
    private static final Logger log = LoggerFactory.getLogger(TardisService.class);
    private final ScheduledEventRepository scheduleRepository;
    private final ClockPort clockPort;
    private final ApplicationEventPublisher eventPublisher;

    public TardisService(ScheduledEventRepository scheduleRepository, 
                         ClockPort clockPort, 
                         ApplicationEventPublisher eventPublisher) {
        this.scheduleRepository = scheduleRepository;
        this.clockPort = clockPort;
        this.eventPublisher = eventPublisher;
    }

    public EntitySelfAwareness getAwareness(EntityTemporalContext context) {
        LocalDateTime now = clockPort.now();
        long years = ChronoUnit.YEARS.between(context.birthTimestamp(), now);
        
        LifeStage lifeStage = LifeStage.UNKNOWN;
        if (context.type() == EntityType.PERSON) {
            if (years < 25) lifeStage = LifeStage.YOUNG;
            else if (years < 60) lifeStage = LifeStage.MIDDLE_AGED;
            else lifeStage = LifeStage.OLD;
        }

        // Simple Circadian Logic: Daylight from 06:00 to 18:00
        int hour = now.getHour();
        boolean isDaylight = hour >= 6 && hour < 18;

        return new EntitySelfAwareness(
            context.entityId(),
            years + " years",
            lifeStage,
            isDaylight,
            now.plusHours(1), // Next placeholder transition
            context.sourceConfidence()
        );
    }

    @Transactional
    public void registerSchedule(FutureSchedule schedule) {
        log.info("Registering schedule {} for module {}", schedule.scheduleId(), schedule.ownerModule());
        scheduleRepository.save(ScheduledEventEntity.fromRecord(schedule));
    }

    @ApplicationModuleListener
    void onScheduleRequest(ScheduleRequestEvent event) {
        registerSchedule(event.schedule());
    }

    @ApplicationModuleListener
    void onAwarenessRequest(AwarenessRequestEvent event) {
        log.info("Async Awareness requested for entity: {}", event.context().entityId());
        EntitySelfAwareness awareness = getAwareness(event.context());
        eventPublisher.publishEvent(new EntitySelfAwarenessReported(awareness));
    }

    public EmailChainMetadata analyzeEmailChain(String chainId, List<LocalDateTime> points) {
        if (points.isEmpty()) return new EmailChainMetadata(chainId, points, Duration.ZERO, ConfidenceLevel.SPECULATIVE);
        
        List<LocalDateTime> sortedPoints = points.stream().sorted().toList();
        long totalMillis = 0;
        for (int i = 1; i < sortedPoints.size(); i++) {
            totalMillis += Duration.between(sortedPoints.get(i-1), sortedPoints.get(i)).toMillis();
        }
        
        Duration avg = Duration.ofMillis(totalMillis / Math.max(1, sortedPoints.size() - 1));
        return new EmailChainMetadata(chainId, sortedPoints, avg, ConfidenceLevel.HIGHEST);
    }
}
