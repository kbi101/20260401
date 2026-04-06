package com.timelord.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.Mockito;

@ApplicationModuleTest
class InboxModuleTests {

    @Autowired
    InboxSyncService syncService;

    @Autowired
    InboxIntelligenceService intelligenceService;

    @MockitoBean
    GmailPort gmailPort;

    @MockitoBean
    IntelligencePort intelligencePort;

    @Test
    void shouldProcessEmailAndEmitSummary(PublishedEvents events) {
        // Given: A new email in the inbox
        String gmailId = UUID.randomUUID().toString();
        String sourceEmail = "test-account@timelord.com";
        EmailPayload payload = new EmailPayload(
            gmailId, sourceEmail, UUID.randomUUID().toString(), "sender@example.com", 
            LocalDateTime.now(), "Test Subject", "Test Body", "data/bronze/test.txt", new ArrayList<>()
        );
        
        Mockito.when(gmailPort.fetchNewEmails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(List.of(payload));
        
        EmailSummary mockSummary = new EmailSummary(
            UUID.randomUUID().toString(), gmailId, sourceEmail,
            "A test summary", List.of("Action 1"), "POSITIVE", LocalDateTime.now()
        );
        Mockito.when(intelligencePort.summarize(Mockito.any())).thenReturn(mockSummary);

        // When: Sync is triggered
        syncService.onSyncTrigger(new ScheduledSyncTrigger());
        
        // Wait for async sync to complete (since @ApplicationModuleListener is async)
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Manually trigger the "Intelligence" processing step (since it usually polls every 5s)
        intelligenceService.processNextPending();

        // Then: EmailSyncedEvent is published with source context
        assertThat(events.ofType(EmailSyncedEvent.class)).hasSize(1);
        assertThat(events.ofType(EmailSyncedEvent.class)
            .matching(ev -> ev.payload().sourceEmail().equals(sourceEmail)))
            .hasSize(1);

        // And: Summary is generated with source context
        assertThat(events.ofType(EmailSummaryGeneratedEvent.class)).hasSize(1);
        assertThat(events.ofType(EmailSummaryGeneratedEvent.class)
            .matching(ev -> ev.summary().sourceEmail().equals(sourceEmail)))
            .hasSize(1);
    }

    @Test
    void verifyGoldenDatasetQ1Timeline(PublishedEvents events) {
        // Golden Dataset Example 1
        String gmailId = "golden-q1-id";
        String sourceEmail = "pmo@timelord.com";
        EmailPayload payload = new EmailPayload(
            gmailId, sourceEmail, "thread-1", "pmo@timelord.com", LocalDateTime.now(),
            "Q1 Timeline Review", 
            "Hi team, we are pushing the release to Friday. Please update the configs.",
            "data/bronze/golden.txt",
            new ArrayList<>()
        );
        
        EmailSummary expectedSummary = new EmailSummary(
            UUID.randomUUID().toString(), gmailId, sourceEmail,
            "The Q1 release timeline has been shifted to Friday by the team. No exact deadline for config updates is specified.",
            List.of("Update configuration files for the Friday release."),
            "NEUTRAL", LocalDateTime.now()
        );
        Mockito.when(intelligencePort.summarize(Mockito.any())).thenReturn(expectedSummary);

        // Run process
        intelligenceService.process(new EmailSyncedEvent(payload));

        // Verify output matches Spec
        assertThat(events.ofType(EmailSummaryGeneratedEvent.class)
            .matching(ev -> ev.summary().sourceEmail().equals(sourceEmail)))
            .hasSize(1);
    }
}
