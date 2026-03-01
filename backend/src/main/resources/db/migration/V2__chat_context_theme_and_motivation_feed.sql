ALTER TABLE users
    ADD COLUMN full_name VARCHAR(255);

UPDATE users
SET full_name = split_part(email, '@', 1)
WHERE full_name IS NULL OR btrim(full_name) = '';

ALTER TABLE users
    ALTER COLUMN full_name SET NOT NULL;

ALTER TABLE goals
    ADD COLUMN theme_color VARCHAR(16) NOT NULL DEFAULT '#dff6e5';

ALTER TABLE motivation_images
    ADD COLUMN generated_by VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN favorited_at TIMESTAMPTZ;

CREATE TABLE motivation_quotes (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    quote_text VARCHAR(2000) NOT NULL,
    quote_author VARCHAR(255) NOT NULL,
    quote_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (goal_id, quote_date)
);

CREATE INDEX idx_motivation_quotes_goal_date ON motivation_quotes(goal_id, quote_date);
