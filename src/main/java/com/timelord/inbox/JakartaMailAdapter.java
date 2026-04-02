package com.timelord.inbox;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Jakarta Mail implementation of GmailPort using IMAP over SSL/993 with App Passwords.
 */
@Component
class JakartaMailAdapter implements GmailPort {

    private static final Logger log = LoggerFactory.getLogger(JakartaMailAdapter.class);

    @Override
    public List<EmailPayload> fetchNewEmails(String emailAddress, String appPassword, LocalDateTime since) {
        log.info("Fetching emails for {} since {}", emailAddress, since);
        List<EmailPayload> payloads = new ArrayList<>();
        
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.timeout", "10000");

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", emailAddress, appPassword);
            log.info("Connected to IMAP for {}", emailAddress);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Date sinceDate = Date.from(since.atZone(ZoneId.systemDefault()).toInstant());
            SearchTerm term = new ReceivedDateTerm(ComparisonTerm.GT, sinceDate);
            
            Message[] messages = inbox.search(term);
            log.info("Found {} messages since {}", messages.length, since);

            for (Message msg : messages) {
                try {
                    String messageId = (msg.getHeader("Message-ID") != null && msg.getHeader("Message-ID").length > 0) 
                        ? msg.getHeader("Message-ID")[0] : "no-id-" + System.currentTimeMillis();
                        
                    String rawBody = msg.getSubject() + " (body-failed)";
                    try {
                        Object content = msg.getContent();
                        rawBody = (content != null) ? content.toString() : "";
                    } catch (Exception ce) {
                        log.warn("Could not extract body content for subject: {}", msg.getSubject());
                    }
                    
                    String safeId = messageId.replaceAll("[^a-zA-Z0-9.-]", "_");
                    String localPath = "data/bronze/" + safeId + ".txt";
                    try {
                        java.nio.file.Files.writeString(java.nio.file.Paths.get(localPath), rawBody);
                    } catch (Exception fe) {
                        log.error("Failed to write email to temporary folder: {}", localPath, fe);
                        localPath = "FAILED_TO_WRITE";
                    }

                    if (rawBody.length() > 4000) {
                        rawBody = rawBody.substring(0, 4000) + "... (truncated)";
                    }
                        
                    payloads.add(new EmailPayload(
                        messageId,
                        emailAddress,
                        "thread-context",
                        msg.getFrom() != null && msg.getFrom().length > 0 ? msg.getFrom()[0].toString() : "Unknown",
                         msg.getReceivedDate() != null 
                            ? msg.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() 
                            : LocalDateTime.now(),
                        msg.getSubject() != null ? msg.getSubject() : "(No Subject)",
                        rawBody,
                        localPath,
                        new ArrayList<>()
                    ));
                } catch (Exception e) {
                    log.error("Error parsing message content for {}", msg.getSubject(), e);
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            log.error("IMAP Fetch failed for {}", emailAddress, e);
            throw new RuntimeException("IMAP Fetch failed", e);
        }
        
        return payloads;
    }

    @Override
    public void archiveEmail(String emailAddress, String appPassword, String gmailId) {
        log.info("ARCHIVING via IMAP: {} for {}", gmailId, emailAddress);
    }

    @Override
    public List<EmailPayload> fetchNewEmails(LocalDateTime since) {
        return fetchNewEmails("primary@timelord.com", "mock-pass", since);
    }
    
    @Override
    public void archiveEmail(String gmailId) {
        archiveEmail("primary@timelord.com", "mock-pass", gmailId);
    }
}
