# Capability Brief: Email Summarization and Storage

This specification defines the architectural requirements for the `inbox-intelligence` module. All implementations must adhere to the `timelord` architecture constraints, specifically focusing on Event-Driven decoupled interaction between the email fetching logic and the LLM summarization logic.

## 1. Feature Intent (Job-to-be-Done)
- **Goal:** To automate the extraction of insights from multiple user-defined Gmail accounts by periodically polling for new messages via **IMAP/SSL**, summarizing received emails and their attachments, archiving the original message after processing, and providing persistent, searchable summaries.
- **Protocol:** The system MUST use **IMAPS (993)** with **Google App Passwords** for authentication, bypassing the direct Gmail REST API (OAuth2) for ease of local credential management.
- **Architecture (Medallion Model):** 
    - **Bronze Layer:** Raw email bodies are landed as unique `.txt` files in `data/bronze/` and metadata is stored in SQLite with a `PENDING` status.
    - **Silver/Gold Layer:** Refined AI summaries and structured insights are stored in the `email_summaries` table.
- **Multi-Account Support:** The system MUST support parallel or sequential synchronization for an arbitrary number of registered email accounts.
- **Delta Ingestion:** The system MUST track the `lastSuccessfulSyncAt` timestamp independently for EACH registered email account.
- **Manual Control:** The feature MUST provide a REST API to query sync status per account and manually trigger a new sync cycle for any specific account.
- **Target Modulith Context:** `inbox-intelligence` (or sub-modules `email-connector` and `knowledge-base`).

## 2. Event Contracts (Spring Application Events)
To ensure modularity, the system uses the following event-driven contract:

- **Inbound Events:**
    - `ScheduledSyncTrigger`: Fired periodically (e.g., via Spring `@Scheduled`) to initiate the delta sync process.
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
    List<EntityMention> entities,
    LocalDateTime processedAt
) {}

public record SyncState(
    String emailAddress,  // PRIMARY KEY: Unique per account
    String appPassword,   // Google App Password (encrypted in prod)
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
| **Subj:** Q1 Timeline Review. **Body:** "Hi team, we are pushing the release to Friday. Please update the configs." | The Q1 release timeline has been shifted to Friday. | - Update configuration files for the Friday release. |
| **Subj:** Invoice 1234. **Body:** "Attached is the bill for the consultancy. Pay by Monday." (+ PDF attached) | A consultancy invoice (ID: 1234) has been received with a payment deadline of Monday. | - Process payment for Invoice 1234 by Monday. |

## 6. REST API Interface (External Control)
Exposure of functionality via `RestClient` compatible Spring REST controllers.

| Endpoint | Method | Purpose | Payload/Response |
| :--- | :--- | :--- | :--- |
| `/api/v1/inbox/sync-state` | `GET` | Retrieve the delta ingestion state for all accounts. | `List<SyncState>` |
| `/api/v1/inbox/sync` | `POST` | Manually trigger a sync. Optionally pass `email` query param for specific account. | Status 202 (Accepted) |
| `/api/v1/inbox/summaries/{gmailId}` | `GET` | Retrieve the summary for a specific message (regardless of source). | `EmailSummary` |

---
