ALTER TABLE wallet_transactions
    ADD COLUMN penalty_date DATE;

CREATE UNIQUE INDEX idx_wallet_transactions_goal_penalty_date_unique
    ON wallet_transactions(goal_id, penalty_date)
    WHERE type = 'DAILY_PENALTY' AND goal_id IS NOT NULL AND penalty_date IS NOT NULL;

CREATE INDEX idx_wallet_transactions_penalty_date ON wallet_transactions(penalty_date);
