package com.timelord.inbox;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
class InboxIntelligenceService {
    private final IntelligencePort intelligencePort;
    private final EmailPayloadRepository payloadRepository;
    private final EmailSummaryRepository summaryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InboxIntelligenceService(IntelligencePort intelligencePort, 
                                     EmailPayloadRepository payloadRepository,
                                     EmailSummaryRepository summaryRepository, 
                                     ApplicationEventPublisher eventPublisher) {
        this.intelligencePort = intelligencePort;
        this.payloadRepository = payloadRepository;
        this.summaryRepository = summaryRepository;
        this.eventPublisher = eventPublisher;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 5000) // Poll every 5s
    public void processNextPending() {
        payloadRepository.findAll().stream()
            .filter(p -> "PENDING".equals(p.getStatus()))
            .findFirst() // Take exactly one to honor Ollama semaphore/concurrency
            .ifPresent(p -> process(new EmailSyncedEvent(p.toRecord())));
    }

    public void process(EmailSyncedEvent event) {
        EmailPayload payload = event.payload();
        String localContent = payload.bodyContent();
        
        // Finalize Medallion Model: Take a file from Bronze, summarize it, then delete it.
        if (payload.localBodyPath() != null && !"FAILED_TO_WRITE".equals(payload.localBodyPath())) {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(payload.localBodyPath());
                if (java.nio.file.Files.exists(path)) {
                    localContent = java.nio.file.Files.readString(path);
                }
            } catch (Exception e) {
                 // Fallback to the truncated body in memory if file fails.
            }
        }

        try {
            // Update the in-memory payload with the full content read from the folder
            EmailPayload finalPayload = new EmailPayload(
                payload.gmailId(), payload.sourceEmail(), payload.threadId(), payload.sender(),
                payload.receivedAt(), payload.subject(), localContent, payload.localBodyPath(), payload.attachments()
            );

            EmailSummary summary = intelligencePort.summarize(finalPayload);
            summaryRepository.save(EmailSummaryEntity.fromRecord(summary));
            
            payloadRepository.findById(payload.gmailId()).ifPresent(p -> {
                p.setStatus("PROCESSED");
                payloadRepository.save(p);
            });

            // Cleanup Bronze Layer after refinement to Silver/Gold
            if (payload.localBodyPath() != null && !"FAILED_TO_WRITE".equals(payload.localBodyPath())) {
                try {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(payload.localBodyPath()));
                } catch (Exception e) {
                    // Log but don't fail the summary
                }
            }

            eventPublisher.publishEvent(new EmailSummaryGeneratedEvent(summary));
            eventPublisher.publishEvent(new EmailArchivedEvent(payload.gmailId(), payload.sourceEmail()));
            
        } catch (Exception e) {
            payloadRepository.findById(payload.gmailId()).ifPresent(p -> {
                p.setStatus("FAILED");
                payloadRepository.save(p);
            });
            eventPublisher.publishEvent(new ProcessingFailedEvent(payload.gmailId(), e.getMessage()));
        }
    }
}
