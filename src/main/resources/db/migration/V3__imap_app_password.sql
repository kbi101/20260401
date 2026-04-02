-- Phase 3: Credentials storage for App Passwords
ALTER TABLE sync_state ADD COLUMN app_password VARCHAR(255);
-- Note: In production, this would be encrypted; for now, we provide the field.
