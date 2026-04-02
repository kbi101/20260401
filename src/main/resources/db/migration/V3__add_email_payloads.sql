CREATE TABLE email_payloads (
    gmail_id VARCHAR(255) PRIMARY KEY,
    source_email VARCHAR(255) NOT NULL,
    thread_id VARCHAR(255),
    sender VARCHAR(255),
    received_at TIMESTAMP,
    subject TEXT,
    body_content CLOB,
    local_body_path VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING'
);
