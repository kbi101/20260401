package com.timelord.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.UUID;

@org.springframework.modulith.test.ApplicationModuleTest
@org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier
@org.springframework.context.annotation.Import(TimelordInboxBase.TestConfig.class)
@ActiveProfiles("test")
public abstract class TimelordInboxBase {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private InboxIntelligenceService intelligenceService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private IntelligencePort intelligencePort;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private GmailPort gmailPort;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean("email-summaries")
        public org.springframework.integration.channel.QueueChannel emailSummariesChannel() {
            return new org.springframework.integration.channel.QueueChannel();
        }

        @org.springframework.context.annotation.Bean
        public EventBridge eventBridge(org.springframework.integration.channel.QueueChannel emailSummariesChannel) {
            return new EventBridge(emailSummariesChannel);
        }
    }

    static class EventBridge {
        private final org.springframework.integration.channel.QueueChannel channel;
        public EventBridge(org.springframework.integration.channel.QueueChannel channel) {
            this.channel = channel;
        }

        @org.springframework.context.event.EventListener
        public void handle(EmailSummaryGeneratedEvent event) {
            channel.send(org.springframework.messaging.support.MessageBuilder.withPayload(event.summary()).build());
        }
    }

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("email-summaries")
    private org.springframework.messaging.MessageChannel emailSummariesChannel;

    @BeforeEach
    public void setup() {
        // Mock the intelligence port to return what the contract expects (Golden Dataset aligned)
        org.mockito.Mockito.when(intelligencePort.summarize(org.mockito.Mockito.any()))
            .thenReturn(new EmailSummary(
                UUID.randomUUID().toString(),
                "gmail-1",
                "user@example.com",
                "The Q1 release timeline has been shifted to Friday by the team. No exact deadline for config updates is specified.",
                java.util.List.of("Update configuration files for the Friday release."),
                "NEUTRAL",
                LocalDateTime.now()
            ));
    }

    protected void EmailSyncedEvent() {
        // This method is called by the generated contract test
        // It triggers the inbound event that the contract test is testing the fallout of
        EmailPayload payload = new EmailPayload(
            UUID.randomUUID().toString(),
            "user@example.com",
            "thread-123",
            "sender@example.com",
            LocalDateTime.now(),
            "Test Subject",
            "Verification content",
            "data/bronze/test.txt",
            java.util.List.of()
        );
        
        eventPublisher.publishEvent(new EmailSyncedEvent(payload));
        
        // Manually process it to trigger the side-effect (SummaryGeneratedEvent)
        // which the contract test is listening for.
        intelligenceService.process(new EmailSyncedEvent(payload));
    }
}
