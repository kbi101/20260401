-- Phase 2: Structural migration for Multi-Account support
ALTER TABLE sync_state ADD COLUMN email_address VARCHAR(255);
ALTER TABLE sync_state ADD COLUMN account_name VARCHAR(100);

-- Note: We maintain a default 'primary' email for existing V1 data if needed
UPDATE sync_state SET email_address = user_id WHERE email_address IS NULL;

-- Adding source tracking to summaries
ALTER TABLE email_summaries ADD COLUMN source_email VARCHAR(255);
UPDATE email_summaries SET source_email = 'primary@timelord.com' WHERE source_email IS NULL;
