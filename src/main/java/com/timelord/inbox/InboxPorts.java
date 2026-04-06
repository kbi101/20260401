package com.timelord.inbox;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ports/Interfaces for external services as per Anti-Corruption Layer requirement.
 */
interface GmailPort {
    List<EmailPayload> fetchNewEmails(LocalDateTime since);
    void archiveEmail(String gmailId);
    
    // Explicit Credential Support (Multi-Account)
    List<EmailPayload> fetchNewEmails(String emailAddress, String appPassword, LocalDateTime since);
    void archiveEmail(String emailAddress, String appPassword, String gmailId);
}

interface IntelligencePort {
    EmailSummary summarize(EmailPayload email);
}

// §8.3: SMTP Port for sending email replies
interface SmtpPort {
    /**
     * Send a reply to an existing email thread.
     * @param emailAddress The source account's email (From address)
     * @param appPassword  The Google App Password for SMTP auth
     * @param originalEmail The original email being replied to
     * @param replyBody    The plain-text reply body
     * @param replyAll     If true, reply to all original recipients
     * @return The Message-ID assigned by Gmail to the sent reply
     */
    String sendReply(String emailAddress, String appPassword, EmailPayload originalEmail, String replyBody, boolean replyAll);
}
