-- Migration to fix Spring Modulith event publication table size for Postgres
-- Jackson serialized events (especially email payloads) often exceed 255 chars
-- Using SET DATA TYPE which is more portable across Postgres and H2
ALTER TABLE IF EXISTS event_publication ALTER COLUMN serialized_event SET DATA TYPE TEXT;
