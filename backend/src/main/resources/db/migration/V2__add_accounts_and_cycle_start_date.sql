CREATE TABLE IF NOT EXISTS app_users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(40) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    last_login_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_app_users_username_lower ON app_users (LOWER(username));
CREATE INDEX IF NOT EXISTS idx_app_users_status_created ON app_users (status, created_at);

CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_user_sessions_token_expiry ON user_sessions (token_hash, expires_at);

ALTER TABLE cycle_items ADD COLUMN IF NOT EXISTS start_date DATE;
UPDATE cycle_items SET start_date = last_completed_date WHERE start_date IS NULL;
ALTER TABLE cycle_items ALTER COLUMN start_date SET NOT NULL;
