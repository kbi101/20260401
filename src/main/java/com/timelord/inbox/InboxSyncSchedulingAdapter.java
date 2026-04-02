package com.timelord.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter to trigger periodic synchronization cycles.
 */
@Component
class InboxSyncSchedulingAdapter {
    private static final Logger log = LoggerFactory.getLogger(InboxSyncSchedulingAdapter.class);
    private final ApplicationEventPublisher eventPublisher;

    public InboxSyncSchedulingAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Trigger a sync every 8 hours.
     */
    @Scheduled(cron = "0 0 */8 * * *")
    public void scheduleSync() {
        log.info("AUTO-TRIGGER: Initiating periodic inbox synchronization.");
        eventPublisher.publishEvent(new ScheduledSyncTrigger());
    }
}
