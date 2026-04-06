package com.timelord.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface SyncStateRepository extends JpaRepository<SyncStateEntity, String> {}

interface EmailSummaryRepository extends JpaRepository<EmailSummaryEntity, String> {
    Optional<EmailSummaryEntity> findByOriginalGmailId(String originalGmailId);
}

interface EmailPayloadRepository extends JpaRepository<EmailPayloadEntity, String> {
    List<EmailPayloadEntity> findByStatus(String status, Pageable pageable);

    // §8.1: Cursor-based feed — find payloads with receivedAt > since, ordered ASC
    List<EmailPayloadEntity> findByReceivedAtAfterOrderByReceivedAtAsc(LocalDateTime since, Pageable pageable);

    // §8.1: Feed without cursor (load from beginning)
    List<EmailPayloadEntity> findAllByOrderByReceivedAtAsc(Pageable pageable);

    // §8.1: Filter by source email with cursor
    List<EmailPayloadEntity> findBySourceEmailAndReceivedAtAfterOrderByReceivedAtAsc(String sourceEmail, LocalDateTime since, Pageable pageable);

    // §8.1: Filter by source email without cursor
    List<EmailPayloadEntity> findBySourceEmailOrderByReceivedAtAsc(String sourceEmail, Pageable pageable);
}
