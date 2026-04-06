package com.timelord.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.UUID;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.springframework.web.context.WebApplicationContext;

@org.springframework.modulith.test.ApplicationModuleTest
@org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier
@org.springframework.context.annotation.Import(TimelordInboxBase.TestConfig.class)
@ActiveProfiles("test")
public abstract class TimelordInboxBase {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private InboxIntelligenceService intelligenceService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private IntelligencePort intelligencePort;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private GmailPort gmailPort;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private SmtpPort smtpPort;

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

    @Autowired
    private EmailPayloadRepository payloadRepository;

    @Autowired
    private EmailSummaryRepository summaryRepository;

    @Autowired
    private SyncStateRepository syncStateRepository;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

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

        // Seed data for the REST contract tests
        payloadRepository.save(EmailPayloadEntity.fromRecord(new EmailPayload(
            "golden-q1-id", "pmo@timelord.com", "thread-1", "pmo@timelord.com", 
            LocalDateTime.parse("2026-04-06T10:00:00"), "Q1 Timeline Review", 
            "Hi team, we are pushing the release to Friday. Please update the configs.", 
            "data/bronze/golden.txt", java.util.List.of()
        )));

        // Update status for the detail test
        var p = payloadRepository.findById("golden-q1-id").get();
        p.setStatus("PROCESSED");
        payloadRepository.save(p);

        // Seed summary for the feed test
        EmailSummaryEntity s = EmailSummaryEntity.fromRecord(new EmailSummary(
            "11111111-2222-3333-4444-555555555555", "golden-q1-id", "pmo@timelord.com", 
            "The Q1 release timeline has been shifted to Friday by the team. No exact deadline for config updates is specified.", 
            java.util.List.of("Update configuration files for the Friday release."), 
            "NEUTRAL", LocalDateTime.parse("2026-04-06T10:05:00")
        ));
        summaryRepository.save(s);

        // Seed sync state for the reply test
        syncStateRepository.save(new SyncStateEntity("pmo@timelord.com", "account1", "pass", LocalDateTime.now(), 1));

        // Mock SMTP port for reply test
        org.mockito.Mockito.when(smtpPort.sendReply(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.any(), org.mockito.Mockito.anyString(), org.mockito.Mockito.anyBoolean()))
                .thenReturn("reply-msg-1");
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
