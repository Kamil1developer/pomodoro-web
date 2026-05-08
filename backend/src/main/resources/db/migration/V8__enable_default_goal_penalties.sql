ALTER TABLE goal_commitments
    ALTER COLUMN money_enabled SET DEFAULT true,
    ALTER COLUMN deposit_amount SET DEFAULT 300,
    ALTER COLUMN daily_penalty_amount SET DEFAULT 10,
    ALTER COLUMN money_status SET DEFAULT 'ACTIVE';

UPDATE goal_commitments
SET money_enabled = true,
    deposit_amount = CASE WHEN deposit_amount IS NULL OR deposit_amount < 10 THEN 300 ELSE deposit_amount END,
    daily_penalty_amount = CASE WHEN daily_penalty_amount IS NULL OR daily_penalty_amount <= 0 THEN 10 ELSE daily_penalty_amount END,
    money_status = CASE WHEN money_status = 'DISABLED' THEN 'ACTIVE' ELSE money_status END,
    updated_at = now()
WHERE money_enabled = false
   OR daily_penalty_amount IS NULL
   OR daily_penalty_amount <= 0
   OR money_status = 'DISABLED';
