CREATE TABLE user_wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance INTEGER NOT NULL DEFAULT 1000,
    initial_balance INTEGER NOT NULL DEFAULT 1000,
    total_added INTEGER NOT NULL DEFAULT 1000,
    total_penalties INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_user_wallets_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_user_wallets_initial_balance_non_negative CHECK (initial_balance >= 0),
    CONSTRAINT chk_user_wallets_total_added_non_negative CHECK (total_added >= 0),
    CONSTRAINT chk_user_wallets_total_penalties_non_negative CHECK (total_penalties >= 0)
);

CREATE TABLE wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wallet_id BIGINT NOT NULL REFERENCES user_wallets(id) ON DELETE CASCADE,
    goal_id BIGINT REFERENCES goals(id) ON DELETE SET NULL,
    commitment_id BIGINT REFERENCES goal_commitments(id) ON DELETE SET NULL,
    type VARCHAR(64) NOT NULL,
    amount INTEGER NOT NULL,
    balance_before INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wallet_transactions_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT chk_wallet_transactions_balance_before_non_negative CHECK (balance_before >= 0),
    CONSTRAINT chk_wallet_transactions_balance_after_non_negative CHECK (balance_after >= 0)
);

ALTER TABLE goal_commitments
    ADD COLUMN deposit_amount INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN daily_penalty_amount INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN total_penalty_charged INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN money_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN money_status VARCHAR(32) NOT NULL DEFAULT 'DISABLED',
    ADD CONSTRAINT chk_goal_commitments_deposit_amount_non_negative CHECK (deposit_amount >= 0),
    ADD CONSTRAINT chk_goal_commitments_daily_penalty_amount_non_negative CHECK (daily_penalty_amount >= 0),
    ADD CONSTRAINT chk_goal_commitments_total_penalty_charged_non_negative CHECK (total_penalty_charged >= 0);

CREATE INDEX idx_user_wallets_user_id ON user_wallets(user_id);
CREATE INDEX idx_user_wallets_status ON user_wallets(status);
CREATE INDEX idx_wallet_transactions_user_id ON wallet_transactions(user_id);
CREATE INDEX idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_transactions_goal_id ON wallet_transactions(goal_id);
CREATE INDEX idx_wallet_transactions_created_at ON wallet_transactions(created_at);

INSERT INTO user_wallets (user_id, balance, initial_balance, total_added, total_penalties, status, created_at, updated_at)
SELECT id, 1000, 1000, 1000, 0, 'ACTIVE', now(), now()
FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM user_wallets WHERE user_wallets.user_id = users.id
);

INSERT INTO wallet_transactions (user_id, wallet_id, type, amount, balance_before, balance_after, reason, created_at)
SELECT user_id, id, 'INITIAL_GRANT', initial_balance, 0, balance, 'Стартовый баланс для виртуальной ответственности.', created_at
FROM user_wallets
WHERE NOT EXISTS (
    SELECT 1 FROM wallet_transactions
    WHERE wallet_transactions.wallet_id = user_wallets.id
      AND wallet_transactions.type = 'INITIAL_GRANT'
);
