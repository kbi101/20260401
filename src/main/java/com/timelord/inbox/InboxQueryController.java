package com.timelord.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Published Query API controller — spec §8.
 * Exposes cursor-based summary feed, email detail retrieval, and reply functionality
 * for external applications (dashboards, mobile apps, Slack bots).
 */
@RestController
@RequestMapping("/api/v1/inbox")
class InboxQueryController {
    private static final Logger log = LoggerFactory.getLogger(InboxQueryController.class);
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final EmailPayloadRepository payloadRepository;
    private final EmailSummaryRepository summaryRepository;
    private final SyncStateRepository syncStateRepository;
    private final SmtpPort smtpPort;
    private final ApplicationEventPublisher eventPublisher;

    InboxQueryController(EmailPayloadRepository payloadRepository,
                         EmailSummaryRepository summaryRepository,
                         SyncStateRepository syncStateRepository,
                         SmtpPort smtpPort,
                         ApplicationEventPublisher eventPublisher) {
        this.payloadRepository = payloadRepository;
        this.summaryRepository = summaryRepository;
        this.syncStateRepository = syncStateRepository;
        this.smtpPort = smtpPort;
        this.eventPublisher = eventPublisher;
    }

    /**
     * §8.1: Cursor-based summary feed.
     * Load summaries newer than the 'since' cursor, sorted by receivedAt ASC.
     */
    @GetMapping("/feed")
    public ResponseEntity<SummaryFeedPage> getFeed(
            @RequestParam(required = false) LocalDateTime since,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String gmailCategory,
            @RequestParam(required = false) String timelordCategory) {

        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        // Fetch limit+1 to determine hasMore
        var pageable = PageRequest.of(0, effectiveLimit + 1);

        List<EmailPayloadEntity> payloads = payloadRepository.findFeed(email, since, gmailCategory, timelordCategory, pageable);

        boolean hasMore = payloads.size() > effectiveLimit;
        List<EmailPayloadEntity> resultPayloads = hasMore ? payloads.subList(0, effectiveLimit) : payloads;

        // Join each payload with its summary (if processed)
        List<EmailSummaryDetail> details = resultPayloads.stream().map(p -> {
            var summary = summaryRepository.findByOriginalGmailId(p.getGmailId());
            return new EmailSummaryDetail(
                    summary.map(s -> s.getSummaryId()).orElse(null),
                    p.getGmailId(),
                    p.getSourceEmail(),
                    p.getSender(),
                    p.getSubject(),
                    p.getReceivedAt(),
                    p.getGmailCategory(),
                    summary.map(s -> s.getTimelordCategory()).orElse(null),
                    summary.map(s -> s.getSummaryText()).orElse(null),
                    summary.map(s -> s.getKeyActionItems()).orElse(List.of()),
                    summary.map(s -> s.getSentiment()).orElse(null),
                    summary.map(s -> s.getProcessedAt()).orElse(null)
            );
        }).toList();

        LocalDateTime nextCursor = resultPayloads.isEmpty() ? since :
                resultPayloads.get(resultPayloads.size() - 1).getReceivedAt();

        return ResponseEntity.ok(new SummaryFeedPage(details, nextCursor, hasMore));
    }

    /**
     * §8.2: Original email detail retrieval.
     * Returns the full Bronze layer email data.
     */
    @GetMapping("/emails/{gmailId}")
    public ResponseEntity<EmailDetail> getEmailDetail(@PathVariable String gmailId) {
        return payloadRepository.findById(gmailId)
                .map(entity -> new EmailDetail(
                        entity.getGmailId(),
                        entity.getSourceEmail(),
                        entity.getThreadId(),
                        entity.getSender(),
                        entity.getReceivedAt(),
                        entity.getSubject(),
                        entity.getGmailCategory(),
                        entity.getBodyContent(),
                        entity.getStatus(),
                        List.of() // Attachments not yet persisted — placeholder per spec
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * §8.3: Reply to email via Gmail SMTP.
     * Sends through the source account that originally received the email.
     */
    @PostMapping("/emails/{gmailId}/reply")
    public ResponseEntity<ReplyResult> replyToEmail(@PathVariable String gmailId,
                                                     @RequestBody ReplyRequest request) {
        // Look up the original email
        Optional<EmailPayloadEntity> payloadOpt = payloadRepository.findById(gmailId);
        if (payloadOpt.isEmpty()) {
            return ResponseEntity.unprocessableEntity().build();
        }

        EmailPayloadEntity payload = payloadOpt.get();
        EmailPayload originalEmail = payload.toRecord();

        // Look up credentials from SyncState
        Optional<SyncStateEntity> syncOpt = syncStateRepository.findById(payload.getSourceEmail());
        if (syncOpt.isEmpty()) {
            log.error("REPLY-FAILED: No sync state found for account {}", payload.getSourceEmail());
            return ResponseEntity.unprocessableEntity().build();
        }

        SyncStateEntity syncState = syncOpt.get();
        String replyMessageId = smtpPort.sendReply(
                syncState.getEmailAddress(),
                syncState.getAppPassword(),
                originalEmail,
                request.body(),
                request.replyAll()
        );

        LocalDateTime sentAt = LocalDateTime.now();
        ReplyResult result = new ReplyResult(gmailId, syncState.getEmailAddress(), replyMessageId, sentAt);

        // Publish audit event
        eventPublisher.publishEvent(new EmailRepliedEvent(gmailId, syncState.getEmailAddress(), replyMessageId, sentAt));

        return ResponseEntity.ok(result);
    }
}
