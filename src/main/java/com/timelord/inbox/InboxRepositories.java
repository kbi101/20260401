package com.timelord.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface SyncStateRepository extends JpaRepository<SyncStateEntity, String> {}

interface EmailSummaryRepository extends JpaRepository<EmailSummaryEntity, String> {
    Optional<EmailSummaryEntity> findByOriginalGmailId(String originalGmailId);
}

interface EmailPayloadRepository extends JpaRepository<EmailPayloadEntity, String> {
    java.util.List<EmailPayloadEntity> findByStatus(String status, org.springframework.data.domain.Pageable pageable);
}
