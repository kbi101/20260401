package com.timelord.inbox;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event contracts and Data Schemas as defined in the Capability Brief.
 */
record EmailPayload(
    String gmailId, 
    String sourceEmail,    // Identifying origin account
    String threadId,
    String sender,
    LocalDateTime receivedAt,
    String subject,
    String bodyContent,
    String localBodyPath,
    List<AttachmentMetadata> attachments
) {}

record AttachmentMetadata(
    String fileId,
    String fileName,
    String contentType,
    long sizeBytes,
    String localCachePath
) {}

record EmailSummary(
    String summaryId,
    String originalGmailId,
    String sourceEmail,    // Linking to source inbox
    String summaryText,
    List<String> keyActionItems,
    String sentiment,
    LocalDateTime processedAt
) {}

record SyncState(
    String emailAddress,   // PRIMARY KEY
    String accountName,
    LocalDateTime lastSuccessfulSyncAt,
    int totalProcessedCount
) {}

// Events
record ScheduledSyncTrigger() {}

record EmailSyncedEvent(EmailPayload payload) {}

record EmailSummaryGeneratedEvent(EmailSummary summary) {}

record EmailArchivedEvent(String gmailId, String sourceEmail) {}

record ProcessingFailedEvent(String gmailId, String reason) {}

record EmailRepliedEvent(String originalGmailId, String sourceEmail, String replyMessageId, LocalDateTime sentAt) {}

// §8.1 Published Query API — Cursor-based summary feed
record EmailSummaryDetail(
    String summaryId,
    String originalGmailId,
    String sourceEmail,
    String sender,
    String subject,
    LocalDateTime receivedAt,
    String summaryText,
    List<String> keyActionItems,
    String sentiment,
    LocalDateTime processedAt
) {}

record SummaryFeedPage(
    List<EmailSummaryDetail> summaries,
    LocalDateTime nextCursor,
    boolean hasMore
) {}

// §8.2 Original email detail retrieval
record EmailDetail(
    String gmailId,
    String sourceEmail,
    String threadId,
    String sender,
    LocalDateTime receivedAt,
    String subject,
    String bodyContent,
    String status,
    List<AttachmentMetadata> attachments
) {}

// §8.3 Reply to email
record ReplyRequest(
    String gmailId,
    String body,
    boolean replyAll
) {}

record ReplyResult(
    String originalGmailId,
    String sourceEmail,
    String replyMessageId,
    LocalDateTime sentAt
) {}
