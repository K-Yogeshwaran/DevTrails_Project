-- Run this in pgAdmin or psql against gigshield_db
-- This creates all tables that Spring Boot hasn't created yet
-- and fixes column lengths

-- 1. Add event_id column to claims if it doesn't exist
ALTER TABLE claims ADD COLUMN IF NOT EXISTS event_id VARCHAR(120);

-- 2. Fix claim_id and policy_number column lengths
ALTER TABLE claims ALTER COLUMN claim_id TYPE VARCHAR(60);
ALTER TABLE claims ALTER COLUMN policy_number TYPE VARCHAR(60);

-- 3. Create trigger_events table
CREATE TABLE IF NOT EXISTS trigger_events (
    id               BIGSERIAL PRIMARY KEY,
    event_id         VARCHAR(120) UNIQUE NOT NULL,
    trigger_type     VARCHAR(30)  NOT NULL,
    zone_id          VARCHAR(60)  NOT NULL,
    zone_name        VARCHAR(60),
    trigger_value    DECIMAL(8,2),
    status           VARCHAR(20)  DEFAULT 'active',
    started_at       TIMESTAMP    NOT NULL,
    ended_at         TIMESTAMP,
    disrupted_hours  DECIMAL(5,2),
    affected_worker_ids TEXT,
    created_at       TIMESTAMP    DEFAULT NOW()
);

-- 4. Create claim_processing_logs table
CREATE TABLE IF NOT EXISTS claim_processing_logs (
    id         BIGSERIAL PRIMARY KEY,
    claim_id   VARCHAR(60)  NOT NULL,
    stage      VARCHAR(30)  NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    detail     TEXT,
    created_at TIMESTAMP    DEFAULT NOW()
);

-- 5. Create index for fast lookup
CREATE INDEX IF NOT EXISTS idx_trigger_events_zone_status
    ON trigger_events(zone_id, status);

CREATE INDEX IF NOT EXISTS idx_claim_logs_claim_id
    ON claim_processing_logs(claim_id);

CREATE INDEX IF NOT EXISTS idx_claims_event_id
    ON claims(event_id);
