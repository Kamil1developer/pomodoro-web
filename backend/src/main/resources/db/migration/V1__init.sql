CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(3000),
    target_hours NUMERIC(10,2),
    deadline DATE,
    current_streak INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE focus_sessions (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    duration_minutes INTEGER
);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    report_date DATE NOT NULL,
    comment VARCHAR(3000),
    image_path VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    ai_verdict VARCHAR(32),
    ai_explanation VARCHAR(3000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE motivation_images (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    image_path VARCHAR(1000) NOT NULL,
    prompt VARCHAR(3000) NOT NULL,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chat_threads (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    thread_id BIGINT NOT NULL REFERENCES chat_threads(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    content VARCHAR(6000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(2000) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE daily_summaries (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    summary_date DATE NOT NULL,
    completed_tasks INTEGER NOT NULL,
    focus_minutes INTEGER NOT NULL,
    streak INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (goal_id, summary_date)
);

CREATE INDEX idx_goals_user_id ON goals(user_id);
CREATE INDEX idx_tasks_goal_id ON tasks(goal_id);
CREATE INDEX idx_focus_sessions_goal_id ON focus_sessions(goal_id);
CREATE INDEX idx_focus_sessions_goal_started_at ON focus_sessions(goal_id, started_at);
CREATE INDEX idx_reports_goal_id ON reports(goal_id);
CREATE INDEX idx_reports_status_report_date ON reports(status, report_date);
CREATE INDEX idx_motivation_goal_id ON motivation_images(goal_id);
CREATE INDEX idx_chat_threads_goal_id ON chat_threads(goal_id);
CREATE INDEX idx_chat_messages_thread_id ON chat_messages(thread_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_daily_summaries_goal_date ON daily_summaries(goal_id, summary_date);
