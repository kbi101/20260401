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

    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM EmailPayloadEntity p " +
        "LEFT JOIN EmailSummaryEntity s ON p.gmailId = s.originalGmailId " +
        "WHERE (:email IS NULL OR p.sourceEmail = :email) " +
        "  AND (CAST(:since AS timestamp) IS NULL OR p.receivedAt > :since) " +
        "  AND (:gmailCategory IS NULL OR p.gmailCategory = :gmailCategory) " +
        "  AND (:timelordCategory IS NULL OR s.timelordCategory = :timelordCategory) " +
        "ORDER BY p.receivedAt ASC"
    )
    List<EmailPayloadEntity> findFeed(
        @org.springframework.data.repository.query.Param("email") String email,
        @org.springframework.data.repository.query.Param("since") LocalDateTime since,
        @org.springframework.data.repository.query.Param("gmailCategory") String gmailCategory,
        @org.springframework.data.repository.query.Param("timelordCategory") String timelordCategory,
        Pageable pageable
    );
}
