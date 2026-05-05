ALTER TABLE users
    ADD COLUMN avatar_path VARCHAR(1000);

ALTER TABLE goals
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN completed_at TIMESTAMPTZ,
    ADD COLUMN closed_at TIMESTAMPTZ,
    ADD COLUMN failure_reason VARCHAR(3000);

UPDATE goals
SET status = 'ACTIVE'
WHERE status IS NULL;

CREATE INDEX idx_goals_user_status ON goals(user_id, status);
