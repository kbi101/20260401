CREATE TABLE sync_state (
    user_id VARCHAR(255) PRIMARY KEY,
    last_successful_sync_at TIMESTAMP,
    total_processed_count INT DEFAULT 0
);

CREATE TABLE email_summaries (
    summary_id VARCHAR(255) PRIMARY KEY,
    original_gmail_id VARCHAR(255) UNIQUE NOT NULL,
    summary_text TEXT NOT NULL,
    key_action_items TEXT, -- JSON or comma separated
    sentiment VARCHAR(50),
    processed_at TIMESTAMP NOT NULL
);
