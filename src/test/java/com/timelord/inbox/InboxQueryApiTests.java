package com.timelord.inbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InboxQueryController.class)
class InboxQueryApiTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmailPayloadRepository payloadRepository;

    @MockitoBean
    EmailSummaryRepository summaryRepository;

    @MockitoBean
    SyncStateRepository syncStateRepository;

    @MockitoBean
    SmtpPort smtpPort;

    @MockitoBean
    ApplicationEventPublisher eventPublisher;

    @Test
    void testGetFeed() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        EmailPayloadEntity payload = EmailPayloadEntity.fromRecord(new EmailPayload(
                "gmail-1", "user@gmail.com", "thread-1", "sender@test.com", now, 
                "Subject", "Body", "path", List.of(), "Primary"));
        
        when(payloadRepository.findFeed(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(payload));
        
        when(summaryRepository.findByOriginalGmailId("gmail-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/inbox/feed?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaries[0].originalGmailId").value("gmail-1"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void testGetEmailDetail() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        EmailPayloadEntity payload = EmailPayloadEntity.fromRecord(new EmailPayload(
                "gmail-1", "user@gmail.com", "thread-1", "sender@test.com", now, 
                "Subject", "Body", "path", List.of(), "Primary"));

        when(payloadRepository.findById("gmail-1")).thenReturn(Optional.of(payload));

        mockMvc.perform(get("/api/v1/inbox/emails/gmail-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmailId").value("gmail-1"))
                .andExpect(jsonPath("$.bodyContent").value("Body"));
    }

    @Test
    void testReplyToEmail() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        EmailPayloadEntity payload = EmailPayloadEntity.fromRecord(new EmailPayload(
                "gmail-1", "user@gmail.com", "thread-1", "sender@test.com", now, 
                "Subject", "Body", "path", List.of(), "Primary"));

        SyncStateEntity syncState = new SyncStateEntity("user@gmail.com", "account1", "pass", now, 1);

        when(payloadRepository.findById("gmail-1")).thenReturn(Optional.of(payload));
        when(syncStateRepository.findById("user@gmail.com")).thenReturn(Optional.of(syncState));
        when(smtpPort.sendReply(anyString(), anyString(), any(), anyString(), anyBoolean()))
                .thenReturn("reply-msg-1");

        mockMvc.perform(post("/api/v1/inbox/emails/gmail-1/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gmailId\":\"gmail-1\",\"body\":\"rebound\",\"replyAll\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyMessageId").value("reply-msg-1"));
    }
}
