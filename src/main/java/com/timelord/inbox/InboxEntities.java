package com.timelord.inbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sync_state")
class SyncStateEntity {

    @Id
    @Column(name = "user_id")
    private String emailAddress;
    private String accountName;
    private String appPassword;
    private LocalDateTime lastSuccessfulSyncAt;
    private int totalProcessedCount;

    protected SyncStateEntity() {}

    public SyncStateEntity(String emailAddress, String accountName, String appPassword, LocalDateTime lastSuccessfulSyncAt, int totalProcessedCount) {
        this.emailAddress = emailAddress;
        this.accountName = accountName;
        this.appPassword = appPassword;
        this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
        this.totalProcessedCount = totalProcessedCount;
    }

    public String getEmailAddress() { return emailAddress; }
    public String getAccountName() { return accountName; }
    public String getAppPassword() { return appPassword; }
    public LocalDateTime getLastSuccessfulSyncAt() { return lastSuccessfulSyncAt; }
    public int getTotalProcessedCount() { return totalProcessedCount; }

    public void setLastSuccessfulSyncAt(LocalDateTime lastSuccessfulSyncAt) {
        this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
    }

    public void incrementProcessedCount() {
        this.totalProcessedCount++;
    }
}

@Entity
@Table(name = "email_summaries")
class EmailSummaryEntity {
    @Id
    private String summaryId;
    private String originalGmailId;
    private String sourceEmail;
    
    @Column(columnDefinition = "TEXT")
    private String summaryText;
    
    @ElementCollection
    private List<String> keyActionItems;
    
    private String sentiment;
    private String timelordCategory;
    private LocalDateTime processedAt;

    protected EmailSummaryEntity() {}

    public static EmailSummaryEntity fromRecord(EmailSummary record) {
        EmailSummaryEntity entity = new EmailSummaryEntity();
        entity.summaryId = record.summaryId();
        entity.originalGmailId = record.originalGmailId();
        entity.sourceEmail = record.sourceEmail();
        entity.summaryText = record.summaryText();
        entity.keyActionItems = record.keyActionItems();
        entity.sentiment = record.sentiment();
        entity.timelordCategory = record.timelordCategory();
        entity.processedAt = record.processedAt();
        return entity;
    }

    public EmailSummary toRecord() {
        return new EmailSummary(summaryId, originalGmailId, sourceEmail, summaryText, keyActionItems, sentiment, timelordCategory, processedAt);
    }

    public String getSummaryId() { return summaryId; }
    public String getOriginalGmailId() { return originalGmailId; }
    public String getSourceEmail() { return sourceEmail; }
    public String getSummaryText() { return summaryText; }
    public List<String> getKeyActionItems() { return keyActionItems; }
    public String getSentiment() { return sentiment; }
    public String getTimelordCategory() { return timelordCategory; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}

@Entity
@Table(name = "email_payloads")
class EmailPayloadEntity {
    @Id
    private String gmailId;
    private String sourceEmail;
    private String threadId;
    private String sender;
    private LocalDateTime receivedAt;
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String bodyContent;
    
    private String localBodyPath;
    private String status; // PENDING, PROCESSED, FAILED
    private String gmailCategory;

    protected EmailPayloadEntity() {}

    public static EmailPayloadEntity fromRecord(EmailPayload record) {
        EmailPayloadEntity entity = new EmailPayloadEntity();
        entity.gmailId = record.gmailId();
        entity.sourceEmail = record.sourceEmail();
        entity.threadId = record.threadId();
        entity.sender = record.sender();
        entity.receivedAt = record.receivedAt();
        entity.subject = record.subject();
        entity.bodyContent = record.bodyContent();
        entity.localBodyPath = record.localBodyPath();
        entity.gmailCategory = record.gmailCategory();
        entity.status = "PENDING";
        return entity;
    }

    public EmailPayload toRecord() {
        return new EmailPayload(gmailId, sourceEmail, threadId, sender, receivedAt, subject, bodyContent, localBodyPath, List.of(), gmailCategory);
    }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public String getGmailId() { return gmailId; }
    public String getSourceEmail() { return sourceEmail; }
    public String getThreadId() { return threadId; }
    public String getSender() { return sender; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public String getSubject() { return subject; }
    public String getBodyContent() { return bodyContent; }
    public String getLocalBodyPath() { return localBodyPath; }
    public String getGmailCategory() { return gmailCategory; }
}
