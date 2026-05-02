CREATE TABLE goal_commitments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    daily_target_minutes INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(32) NOT NULL,
    discipline_score INTEGER NOT NULL DEFAULT 80,
    current_streak INTEGER NOT NULL DEFAULT 0,
    best_streak INTEGER NOT NULL DEFAULT 0,
    completed_days INTEGER NOT NULL DEFAULT 0,
    missed_days INTEGER NOT NULL DEFAULT 0,
    personal_reward_title VARCHAR(255),
    personal_reward_description VARCHAR(3000),
    reward_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    risk_status VARCHAR(32) NOT NULL DEFAULT 'LOW',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_goal_commitments_daily_target_minutes CHECK (daily_target_minutes > 0),
    CONSTRAINT chk_goal_commitments_discipline_score CHECK (discipline_score BETWEEN 0 AND 100),
    CONSTRAINT chk_goal_commitments_current_streak CHECK (current_streak >= 0),
    CONSTRAINT chk_goal_commitments_best_streak CHECK (best_streak >= 0),
    CONSTRAINT chk_goal_commitments_completed_days CHECK (completed_days >= 0),
    CONSTRAINT chk_goal_commitments_missed_days CHECK (missed_days >= 0)
);

CREATE TABLE goal_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    commitment_id BIGINT REFERENCES goal_commitments(id) ON DELETE SET NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(3000),
    old_value VARCHAR(1000),
    new_value VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE reports
    ADD COLUMN ai_confidence DOUBLE PRECISION;

CREATE INDEX idx_goal_commitments_user_id ON goal_commitments(user_id);
CREATE INDEX idx_goal_commitments_goal_id ON goal_commitments(goal_id);
CREATE INDEX idx_goal_commitments_status ON goal_commitments(status);
CREATE INDEX idx_goal_events_user_goal ON goal_events(user_id, goal_id);
CREATE INDEX idx_goal_events_goal_created_at ON goal_events(goal_id, created_at);
CREATE UNIQUE INDEX idx_goal_commitments_goal_active ON goal_commitments(goal_id) WHERE status = 'ACTIVE';
