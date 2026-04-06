# Capability Brief: Email Summarization and Storage

This specification defines the architectural requirements for the `inbox-intelligence` module. All implementations must adhere to the `timelord` architecture constraints, specifically focusing on Event-Driven decoupled interaction between the email fetching logic and the LLM summarization logic.

## 1. Feature Intent (Job-to-be-Done)
- **Goal:** To automate the extraction of insights from multiple user-defined Gmail accounts by periodically polling for new messages via **IMAP/SSL**, summarizing received emails and their attachments into detailed contextual overviews, archiving the original message after processing, and providing persistent, searchable summaries.
- **Protocol:** The system MUST use **IMAPS (993)** with **Google App Passwords** for authentication, bypassing the direct Gmail REST API (OAuth2) for ease of local credential management. Due to IMAP timezone blind spots with the `SINCE` date term, the system MUST use a batched backward-pagination strategy to manually filter messages by precise `LocalDateTime`.
- **Architecture (Medallion Model):** 
    - **Bronze Layer:** Raw email bodies are landed as unique `.txt` files in `data/bronze/` and metadata is stored in the database with a `PENDING` status.
    - **Silver/Gold Layer:** Refined AI summaries and structured insights are stored in the `email_summaries` table.
- **Database (Dual Support):** 
    - **Production/Primary:** PostgreSQL 17+ (using dedicated `gmail_db` schema).
    - **Local/Development:** SQLite or H2 (for fast-feedback loops).
- **Multi-Account Support:** The system MUST support parallel or sequential synchronization for an arbitrary number of registered email accounts.
- **Delta Ingestion:** The system MUST track the `lastSuccessfulSyncAt` timestamp independently for EACH registered email account.
- **Manual Control:** The feature MUST provide a REST API to query sync status per account and manually trigger a new sync cycle for any specific account.
- **Target Modulith Context:** `inbox-intelligence` (or sub-modules `email-connector` and `knowledge-base`).

## 2. Event Contracts (Spring Application Events)
To ensure modularity, the system uses the following event-driven contract:

- **Inbound Events:**
    - `ScheduledSyncTrigger`: Fired periodically (via Spring `@Scheduled` or startup listener) to initiate the delta sync process. Handled via a **Synchronous `@EventListener`** to ensure immediate execution in the same thread.
    - `EmailSyncedEvent`: Triggered by the `email-connector` when a new email is detected and downloaded.
    - `AttachmentDownloadedEvent`: Emitted after attachments are locally cached and ready for processing.
- **Outbound Events:**
    - `EmailSummaryGeneratedEvent`: Produced after the LLM completes its analysis and the summary is persisted.
    - `EmailArchivedEvent`: Triggered after the summary is saved, indicating the original can now be moved to the "Archive" folder in Gmail.
    - `ProcessingFailedEvent`: Emitted if either the LLM call or the attachment extraction fails, containing a reason code and original MessageID.

## 3. Strict Data Schemas
All data payloads must be implemented as **Java Records** to ensure immutability and Spring Modulith compliance.

### Input Schema (Email Context)
```java
public record EmailPayload(
    String gmailId, 
    String sourceEmail,    // Identifying the origin account
    String threadId,
    String sender,
    LocalDateTime receivedAt,
    String subject,
    String bodyContent,
    String localBodyPath,  // Path to the Bronze file (full body)
    List<AttachmentMetadata> attachments
) {}

public record AttachmentMetadata(
    String fileId,
    String fileName,
    String contentType,
    long sizeBytes,
    String localCachePath
) {}
```

### Output Schema (Summary Storage)
```java
public record EmailSummary(
    String summaryId,
    String originalGmailId,
    String sourceEmail,    // Linking summary back to the specific inbox
    String summaryText,
    List<String> keyActionItems,
    String sentiment,
    LocalDateTime processedAt
) {}

record SyncState(
    String emailAddress,  // PRIMARY KEY: Unique per account
    String accountName,
    LocalDateTime lastSuccessfulSyncAt,
    int totalProcessedCount
) {}
```

## 4. Architecture & Resource Guardrails
- **Decoupled Processing:** Ingestion (IMAP sync) and Refinement (LLM Summarization) MUST be decoupled. Ingestion lands data in the **Bronze** layer, while a separate background worker (Scheduler/Poller) processes **PENDING** payloads into **Silver/Gold** insights.
- **Storage Hygiene:** Once a **Bronze** raw file is successfully refined into a **Silver/Gold** summary, the local raw file MUST be deleted to optimize storage.
- **Local Concurrency Guard:** When utilizing local models (e.g., Ollama), the system MUST restrict concurrent AI calls to exactly **ONE (n=1)** per instance to prevent hardware exhaustion.
- **Timeout Management:** Each AI inference call MUST have an enforced timeout (default: 180s) to prevent thread hangs during local compute spikes.
- **Required Context:** The model must receive the full email body (read from the Bronze file) AND a text extraction from any supported attachments.

## 5. Prohibited Actions
- The LLM MUST NOT hallucinate email senders or dates; these must be sourced strictly from the `EmailPayload`.
- The LLM MUST NOT reveal sensitive credentials (API keys, passwords) found in emails in the final summary.
- **Required Tools/Functions:**
    *   `IMAPArchiveTool`: To move a message to the "[Gmail]/Archive" folder via IMAP `MOVE` command (only after storage confirmation).
    *   `TopicClassifier`: To Categorize emails into (Jobs, Purchases, General) based on our pre-defined keyword lists before LLM processing.

## 6. Evaluation Metrics (The Acceptance Criteria)
- **Automated Metric:** LLM-as-a-judge comparison between the generated summary and the "Golden" reference content.
- **Performance Threshold:** 90% accuracy in identifying "Key Action Items" from complex multi-thread emails.

### Golden Dataset Examples

| Input (Email Subject/Body) | Expected Output (Summary) | Expected Output (Action Items) |
| :--- | :--- | :--- |
| **Subj:** Q1 Timeline Review. **Body:** "Hi team, we are pushing the release to Friday. Please update the configs." | The Q1 release timeline has been shifted to Friday by the team. No exact deadline for config updates is specified. | - Update configuration files for the Friday release. |
| **Subj:** Invoice 1234. **Body:** "Attached is the bill for the consultancy. Pay by Monday." (+ PDF attached) | A consultancy invoice (ID: 1234) has been received with a payment deadline of Monday. | - Process payment for Invoice 1234 by Monday. |

## 7. REST API Interface (Internal Control)
Exposure of sync and management functionality via Spring REST controllers.

| Endpoint | Method | Purpose | Payload/Response |
| :--- | :--- | :--- | :--- |
| `/api/v1/inbox/sync-state` | `GET` | Retrieve the delta ingestion state for all accounts. | `List<SyncState>` |
| `/api/v1/inbox/sync` | `POST` | Manually trigger a sync. Optionally pass `email` query param for specific account. | Status 202 (Accepted) |
| `/api/v1/inbox/summaries/{gmailId}` | `GET` | Retrieve the summary for a specific message (regardless of source). | `EmailSummary` |

## 8. Published Query API (External Consumption)
External applications (dashboards, mobile apps, Slack bots) need to consume inbox intelligence data without re-fetching or duplicating state. All query endpoints are **read-only** and belong to the `inbox` module's **published API surface**.

### 8.1 Cursor-Based Summary Feed
- **Job-to-be-Done:** A consuming application loads email summaries incrementally, using the email's original `receivedAt` timestamp as a cursor. On each call, the consumer passes the timestamp of the *last email it saw*, and receives only newer summaries. This prevents duplicate loading across polling cycles.
- **Cursor Field:** `receivedAt` (from `EmailPayload.receivedAt`, preserved on the summary entity as well). This is the Gmail received timestamp, NOT the `processedAt` timestamp — because the consumer cares about *when the email arrived*, not when the AI processed it.
- **Ordering:** Results MUST be sorted by `receivedAt ASC` (oldest-first) to ensure stable cursor progression.
- **Pagination:** Responses MUST support a `limit` parameter (default: 50, max: 200) to control batch size.

```java
public record SummaryFeedPage(
    List<EmailSummaryDetail> summaries,
    LocalDateTime nextCursor,     // receivedAt of the last item; pass as 'since' on the next call
    boolean hasMore               // true if more results exist beyond the limit
) {}

// Enriched summary record for external consumers
public record EmailSummaryDetail(
    String summaryId,
    String originalGmailId,
    String sourceEmail,
    String sender,                // From the original EmailPayload
    String subject,               // From the original EmailPayload
    LocalDateTime receivedAt,     // The Gmail received time (cursor field)
    String summaryText,
    List<String> keyActionItems,
    String sentiment,
    LocalDateTime processedAt
) {}
```

| Endpoint | Method | Purpose | Query Params |
| :--- | :--- | :--- | :--- |
| `/api/v1/inbox/feed` | `GET` | Load summaries newer than a cursor. | `since` (ISO datetime, optional — omit for all), `limit` (int, default 50), `email` (optional — filter by source account) |

**Example:** `GET /api/v1/inbox/feed?since=2026-04-06T09:00:00&limit=20`

### 8.2 Original Email Detail Retrieval
- **Job-to-be-Done:** After reading a summary, the consumer may need to view the full original email (subject, sender, body, attachments metadata) for context. This serves the **Bronze layer** data.
- **Data Source:** The `email_payloads` table, which stores the raw email metadata and body content.
- **Access Pattern:** Lookup by `gmailId` (the unique IMAP Message-ID).

```java
public record EmailDetail(
    String gmailId,
    String sourceEmail,
    String threadId,
    String sender,
    LocalDateTime receivedAt,
    String subject,
    String bodyContent,           // Full email body (from Bronze layer)
    String status,                // PENDING, PROCESSED, FAILED
    List<AttachmentMetadata> attachments
) {}
```

| Endpoint | Method | Purpose | Payload/Response |
| :--- | :--- | :--- | :--- |
| `/api/v1/inbox/emails/{gmailId}` | `GET` | Retrieve the full original email detail. | `EmailDetail` or 404 |

### 8.3 Reply to Email via Gmail
- **Job-to-be-Done:** A consuming application (e.g., a dashboard or AI agent) can compose and send a reply to a specific email thread via the user's Gmail account. This closes the loop from "read → understand → act" within the Timelord ecosystem.
- **Protocol:** The reply MUST be sent via **SMTP over SSL (port 465)** using the same Google App Password credentials already configured for the source account. The SMTP connection reuses the `SyncState` account credentials.
- **Threading:** The reply MUST set the `In-Reply-To` and `References` headers using the original email's `gmailId` (Message-ID) to ensure Gmail groups the reply into the correct conversation thread.
- **Sender Identity:** The `From` address MUST match the `sourceEmail` of the original email to maintain sender authenticity.
- **Security Constraints:**
    - The reply body MUST NOT include any raw LLM-generated content unless explicitly provided by the user. The system acts as a **transport layer**, not an autonomous responder.
    - The system MUST log every outbound reply (recipient, subject, timestamp) for auditability.

```java
public record ReplyRequest(
    String gmailId,           // The Message-ID of the email being replied to
    String body,              // Plain-text reply body (composed by the user or upstream app)
    boolean replyAll          // If true, reply to all original recipients
) {}

public record ReplyResult(
    String originalGmailId,
    String sourceEmail,       // The account used to send the reply
    String replyMessageId,    // The new Message-ID assigned by Gmail
    LocalDateTime sentAt
) {}
```

| Endpoint | Method | Purpose | Payload/Response |
| :--- | :--- | :--- | :--- |
| `/api/v1/inbox/emails/{gmailId}/reply` | `POST` | Send a reply to a specific email thread. | Request: `ReplyRequest`, Response: `ReplyResult` or 422 (if original email not found) |

### 8.4 Architecture Constraints for Published API
- All query endpoints are **read-only** and MUST NOT trigger sync cycles or AI processing.
- The reply endpoint is the **only write operation** exposed to external consumers.
- All endpoints MUST be implemented within the `inbox` module's public package to comply with Spring Modulith boundaries. External modules MUST NOT directly access `email_payloads`, `email_summaries`, or `sync_state` tables.
- The `EmailSummaryDetail` record enriches the base `EmailSummary` with fields from `EmailPayload` (sender, subject, receivedAt). This join is performed **inside the module** — consumers receive a denormalized view.

---
