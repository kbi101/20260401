# Inbox Intelligence — User & Integration Guide

## Overview

**Inbox Intelligence** is an automated email awareness feature for the Timelord platform. It continuously monitors your Gmail accounts, reads incoming messages, and produces concise, actionable summaries — so you never miss an important deadline, invoice, or follow-up buried in your inbox.

**What problem does it solve?**  
Modern knowledge workers receive hundreds of emails daily. Important action items get lost in noise. Inbox Intelligence acts as a personal assistant that reads every email, distills each one into a brief summary with key action items and sentiment, and stores those insights for instant retrieval — all without you ever opening Gmail.

**Key capabilities:**
- 🔄 **Multi-account support** — Monitor multiple Gmail inboxes simultaneously
- 🧠 **AI-powered summarization** — Each email is analyzed by a local LLM for summary, action items, and sentiment
- 📊 **Searchable summaries** — Query any processed email's summary via a simple REST API
- ⏱️ **Delta sync** — Only new messages are processed; the system remembers where it left off per account

---

## Visual Architecture

The following diagram illustrates the end-to-end flow from email arrival to summary retrieval:

```mermaid
sequenceDiagram
    participant User as 👤 User / Client
    participant API as REST Controller
    participant Scheduler as Sync Scheduler
    participant IMAP as IMAP Adapter<br/>(Gmail)
    participant DB as PostgreSQL
    participant LLM as AI Provider<br/>(Ollama / Qwen)

    Note over Scheduler: Triggers on app startup<br/>and every hour via cron

    Scheduler->>Scheduler: Publish ScheduledSyncTrigger event
    Scheduler->>DB: Read SyncState per account
    Scheduler->>IMAP: Connect via IMAPS (port 993)
    IMAP-->>Scheduler: New emails (batched, 50/page)
    Scheduler->>DB: Save raw payload (Bronze layer)
    Scheduler->>LLM: Summarize email body
    LLM-->>Scheduler: Summary + Action Items + Sentiment
    Scheduler->>DB: Persist EmailSummary (Silver/Gold layer)
    Scheduler->>DB: Update SyncState timestamp

    Note over User,API: Later, the user queries summaries

    User->>API: GET /api/v1/inbox/summaries/{gmailId}
    API->>DB: Lookup by gmailId
    DB-->>API: EmailSummary record
    API-->>User: JSON response

    User->>API: POST /api/v1/inbox/sync
    API->>Scheduler: Trigger immediate sync
    API-->>User: 202 Accepted
```

---

## How It Works

1. **Periodic Polling** — On application startup (and every hour thereafter), the system triggers a sync cycle for every registered Gmail account.
2. **Batched Fetching** — The IMAP adapter connects to Gmail via SSL on port 993, fetches emails in batches of 50, and uses backward-pagination to ensure no messages are missed — even during extended downtime.
3. **Bronze Landing** — Each raw email body is saved as a `.txt` file in `data/bronze/` and its metadata is recorded in the database with a `PENDING` status.
4. **AI Summarization** — A local LLM (Qwen 2.5:7b via Ollama) processes each pending email, extracting a concise summary, key action items, and overall sentiment.
5. **Summary Persistence** — The refined summary is stored in the `email_summaries` table, and the sync state timestamp is advanced.

---

## Integration Details

### REST API Endpoints

| Endpoint | Method | Purpose |
| :--- | :--- | :--- |
| `/api/v1/inbox/sync-state` | `GET` | Retrieve sync status for all accounts |
| `/api/v1/inbox/sync` | `POST` | Manually trigger a sync cycle |
| `/api/v1/inbox/summaries/{gmailId}` | `GET` | Retrieve the AI summary for a specific email |

### Sample: Get Sync State

**Request:**
```http
GET /api/v1/inbox/sync-state HTTP/1.1
Host: localhost:3017
```

**Response (200 OK):**
```json
[
  {
    "emailAddress": "keping.bi@gmail.com",
    "accountName": "personal-gmail",
    "lastSuccessfulSyncAt": "2026-04-06T09:14:16",
    "totalProcessedCount": 147
  }
]
```

### Sample: Get Email Summary

**Request:**
```http
GET /api/v1/inbox/summaries/msg-abc123 HTTP/1.1
Host: localhost:3017
```

**Response (200 OK):**
```json
{
  "summaryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "originalGmailId": "msg-abc123",
  "sourceEmail": "keping.bi@gmail.com",
  "summaryText": "The Q1 release timeline has been shifted to Friday by the team. No exact deadline for config updates is specified.",
  "keyActionItems": [
    "Update configuration files for the Friday release."
  ],
  "sentiment": "NEUTRAL",
  "processedAt": "2026-04-06T09:15:00"
}
```

### Sample: Trigger Manual Sync

**Request:**
```http
POST /api/v1/inbox/sync?email=keping.bi@gmail.com HTTP/1.1
Host: localhost:3017
```

**Response:** `202 Accepted` (sync runs asynchronously)

### Domain Events

Other modules within the Timelord ecosystem can listen for these Spring Application Events:

| Event | When It Fires | Payload |
| :--- | :--- | :--- |
| `EmailSummaryGeneratedEvent` | After a summary is persisted | `EmailSummary` record |
| `ProcessingFailedEvent` | If LLM or extraction fails | Reason code + Message ID |

---

## Module Canvas

The following diagram is auto-generated by Spring Modulith and shows the `inbox` module's dependency graph:

![Module Canvas](../../target/spring-modulith-docs/module-inbox.svg)

---

## Known Limitations

| Limitation | Explanation |
| :--- | :--- |
| **One email at a time** | The AI processes emails sequentially (single concurrency) to avoid overloading local hardware. During high-volume syncs, processing may take several minutes. |
| **180-second timeout** | If the AI model takes longer than 3 minutes to summarize a single email, the request is cancelled to prevent thread hangs. |
| **No OAuth2** | Authentication uses Google App Passwords over IMAP — no browser-based OAuth flow is required, but App Passwords must be configured manually per account. |
| **Attachment support is limited** | Currently, only text-based attachments are extracted. Binary formats (images, videos) are noted but not analyzed. |
| **Summaries are not editable** | Once generated, summaries are immutable. If the AI produces a poor summary, the email must be re-processed. |
| **Local model quality** | The 7B-parameter Qwen model provides good general summaries but may miss nuance in highly technical or domain-specific emails. |
