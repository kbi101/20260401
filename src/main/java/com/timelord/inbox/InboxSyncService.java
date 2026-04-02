package com.timelord.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to orchestrate the synchronization process for multiple accounts.
 */
@Service
class InboxSyncService {
    private static final Logger log = LoggerFactory.getLogger(InboxSyncService.class);
    
    private final GmailPort gmailPort;
    private final SyncStateRepository syncStateRepository;
    private final EmailPayloadRepository emailPayloadRepository;

    public InboxSyncService(GmailPort gmailPort, 
                            SyncStateRepository syncStateRepository, 
                            EmailPayloadRepository emailPayloadRepository) {
        this.gmailPort = gmailPort;
        this.syncStateRepository = syncStateRepository;
        this.emailPayloadRepository = emailPayloadRepository;
    }

    @ApplicationModuleListener
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onSyncTrigger(ScheduledSyncTrigger trigger) {
        log.info("Starting synchronization cycle for all registered accounts.");
        List<SyncStateEntity> allStates = syncStateRepository.findAll();
        
        if (allStates.isEmpty()) {
            log.warn("No accounts found in sync_state. Seeding a default placeholder.");
            allStates.add(new SyncStateEntity("primary@timelord.com", "Primary", "TODO-APP-PASS", LocalDateTime.now().minusDays(1), 0));
        }

        for (SyncStateEntity state : allStates) {
            try {
                log.debug("Syncing account: {}", state.getEmailAddress());
                
                List<EmailPayload> newEmails = gmailPort.fetchNewEmails(
                    state.getEmailAddress(), 
                    state.getAppPassword(), 
                    state.getLastSuccessfulSyncAt()
                );

                log.info("Account {}: Fetched {} new emails.", state.getEmailAddress(), newEmails.size());

                for (EmailPayload email : newEmails) {
                    emailPayloadRepository.save(EmailPayloadEntity.fromRecord(email));
                    state.incrementProcessedCount();
                }

                state.setLastSuccessfulSyncAt(LocalDateTime.now());
                syncStateRepository.save(state);
                log.debug("Successfully updated sync state for {}", state.getEmailAddress());

            } catch (Exception e) {
                log.error("CRITICAL: Failed to sync account {}. Details: {}", state.getEmailAddress(), e.getMessage(), e);
                // We DON'T throw here, so other accounts can continue.
                // Modulith will still mark the trigger as completed since we caught the error.
            }
        }
    }

    @ApplicationModuleListener
    public void onEmailArchived(EmailArchivedEvent event) {
        log.debug("Archive request received for {} in account {}", event.gmailId(), event.sourceEmail());
        syncStateRepository.findById(event.sourceEmail()).ifPresent(state -> {
            try {
                gmailPort.archiveEmail(state.getEmailAddress(), state.getAppPassword(), event.gmailId());
                log.info("Successfully archived {} via IMAP.", event.gmailId());
            } catch (Exception e) {
                log.error("Failed to archive {} via IMAP: {}", event.gmailId(), e.getMessage());
            }
        });
    }
}
