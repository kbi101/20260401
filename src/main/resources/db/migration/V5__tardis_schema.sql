-- Initial schema for TARDIS module
CREATE SCHEMA IF NOT EXISTS tardis_db;

CREATE TABLE IF NOT EXISTS tardis_db.tardis_schedules (
    schedule_id VARCHAR(255) PRIMARY KEY,
    owner_module VARCHAR(100) NOT NULL,
    target_time TIMESTAMP NOT NULL,
    is_periodic BOOLEAN DEFAULT FALSE,
    cron_expression VARCHAR(255),
    payload_json TEXT,
    status VARCHAR(50) DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS tardis_db.tardis_entity_metadata (
    entity_id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    timezone VARCHAR(100),
    birth_timestamp TIMESTAMP,
    confidence VARCHAR(50)
);
