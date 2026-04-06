package com.timelord.inbox;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * SMTP Adapter for sending Gmail replies via SSL (port 465).
 * Implements the SmtpPort anti-corruption layer as per spec §8.3.
 */
@Component
class SmtpAdapter implements SmtpPort {
    private static final Logger log = LoggerFactory.getLogger(SmtpAdapter.class);

    @Override
    public String sendReply(String emailAddress, String appPassword, EmailPayload originalEmail, String replyBody, boolean replyAll) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailAddress, appPassword);
            }
        });

        try {
            MimeMessage reply = new MimeMessage(session);
            reply.setFrom(new InternetAddress(emailAddress));

            // Determine recipients
            if (replyAll) {
                // Reply to sender + all original recipients
                reply.setRecipient(Message.RecipientType.TO, new InternetAddress(originalEmail.sender()));
                // In a full implementation, we'd parse CC recipients from the original email headers
            } else {
                reply.setRecipient(Message.RecipientType.TO, new InternetAddress(originalEmail.sender()));
            }

            // Set threading headers to maintain Gmail conversation
            reply.setSubject("Re: " + originalEmail.subject());
            reply.setHeader("In-Reply-To", originalEmail.gmailId());
            reply.setHeader("References", originalEmail.gmailId());
            reply.setText(replyBody, "utf-8");

            Transport.send(reply);

            String replyMessageId = reply.getMessageID();
            log.info("REPLY-SENT: from={} to={} subject='Re: {}' messageId={}",
                    emailAddress, originalEmail.sender(), originalEmail.subject(), replyMessageId);
            return replyMessageId;

        } catch (MessagingException e) {
            log.error("REPLY-FAILED: from={} to={} error={}", emailAddress, originalEmail.sender(), e.getMessage());
            throw new RuntimeException("Failed to send reply: " + e.getMessage(), e);
        }
    }
}
