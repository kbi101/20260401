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
