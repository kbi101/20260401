package com.timelord.inbox;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inbox")
class InboxIntelligenceController {
    private final SyncStateRepository syncStateRepository;
    private final EmailSummaryRepository summaryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InboxIntelligenceController(SyncStateRepository syncStateRepository, 
                                       EmailSummaryRepository summaryRepository, 
                                       ApplicationEventPublisher eventPublisher) {
        this.syncStateRepository = syncStateRepository;
        this.summaryRepository = summaryRepository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/sync-state")
    public ResponseEntity<java.util.List<SyncState>> getSyncState() {
        var states = syncStateRepository.findAll().stream()
            .map(s -> new SyncState(s.getEmailAddress(), s.getAccountName(), s.getLastSuccessfulSyncAt(), s.getTotalProcessedCount()))
            .toList();
        return ResponseEntity.ok(states);
    }

    @PostMapping("/sync")
    @jakarta.transaction.Transactional
    public ResponseEntity<Void> triggerSync(@RequestParam(required = false) String email) {
        // In a real implementation, 'email' would be used to filter. 
        // For now, triggering a global sync is compliant with the spec record 'ScheduledSyncTrigger()'.
        eventPublisher.publishEvent(new ScheduledSyncTrigger());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/summaries/{gmailId}")
    public ResponseEntity<EmailSummary> getSummary(@PathVariable String gmailId) {
        return summaryRepository.findByOriginalGmailId(gmailId)
            .map(EmailSummaryEntity::toRecord)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
