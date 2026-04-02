package com.timelord.tardis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
class TardisEventPollingAdapter {
    private static final Logger log = LoggerFactory.getLogger(TardisEventPollingAdapter.class);
    private final ScheduledEventRepository repository;
    private final ClockPort clockPort;
    private final ApplicationEventPublisher eventPublisher;

    public TardisEventPollingAdapter(ScheduledEventRepository repository, 
                                     ClockPort clockPort, 
                                     ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.clockPort = clockPort;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndTrigger() {
        LocalDateTime now = clockPort.now();
        List<ScheduledEventEntity> dueSchedules = repository.findByStatus("PENDING").stream()
                .filter(s -> s.getTargetTime().isBefore(now))
                .toList();

        if (dueSchedules.isEmpty()) return;

        log.info("AUTO-TRIGGER: Processing {} due schedules.", dueSchedules.size());
        for (ScheduledEventEntity schedule : dueSchedules) {
            log.info("Triggering schedule {} for {}", schedule.getScheduleId(), schedule.getOwnerModule());
            
            eventPublisher.publishEvent(new ScheduledEventTriggered(
                schedule.getScheduleId(), 
                schedule.getOwnerModule(), 
                schedule.getPayloadJson()
            ));
            
            schedule.setStatus("TRIGGERED");
            repository.save(schedule);
        }
    }
}
