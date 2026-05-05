ALTER TABLE motivation_images
    ADD COLUMN source_url VARCHAR(1000),
    ADD COLUMN title VARCHAR(255),
    ADD COLUMN description VARCHAR(3000),
    ADD COLUMN theme VARCHAR(64),
    ADD COLUMN hidden_globally BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN report_count INTEGER NOT NULL DEFAULT 0;

UPDATE motivation_images
SET source_url = image_path,
    title = COALESCE(title, 'Motivation image'),
    description = COALESCE(description, prompt),
    theme = COALESCE(theme, 'GENERAL')
WHERE source_url IS NULL;

ALTER TABLE motivation_images
    ALTER COLUMN source_url SET NOT NULL,
    ALTER COLUMN title SET NOT NULL,
    ALTER COLUMN theme SET NOT NULL;

ALTER TABLE motivation_images
    ADD CONSTRAINT chk_motivation_images_report_count CHECK (report_count >= 0);

CREATE INDEX idx_motivation_images_theme_created_at ON motivation_images(theme, created_at DESC);
CREATE INDEX idx_motivation_images_hidden_globally ON motivation_images(hidden_globally);
CREATE INDEX idx_motivation_images_report_count ON motivation_images(report_count);

CREATE TABLE motivation_image_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_id BIGINT NOT NULL REFERENCES motivation_images(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL,
    reason VARCHAR(32),
    comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_motivation_image_feedback_user_image_type UNIQUE (user_id, image_id, type)
);

CREATE INDEX idx_motivation_image_feedbacks_user_id ON motivation_image_feedbacks(user_id);
CREATE INDEX idx_motivation_image_feedbacks_image_id ON motivation_image_feedbacks(image_id);
CREATE INDEX idx_motivation_image_feedbacks_type ON motivation_image_feedbacks(type);
CREATE INDEX idx_motivation_image_feedbacks_image_type ON motivation_image_feedbacks(image_id, type);
