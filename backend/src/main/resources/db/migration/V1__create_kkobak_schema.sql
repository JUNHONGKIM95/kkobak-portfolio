CREATE TABLE IF NOT EXISTS cycle_items (
    id VARCHAR(36) PRIMARY KEY,
    owner_key VARCHAR(80) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(40) NOT NULL,
    cycle_type VARCHAR(20) NOT NULL,
    emoji VARCHAR(20) NOT NULL,
    interval_value INTEGER NOT NULL,
    interval_unit VARCHAR(20) NOT NULL,
    last_completed_date DATE NOT NULL,
    next_due_date DATE NOT NULL,
    image_url VARCHAR(2048),
    color VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_cycle_items_owner_due ON cycle_items (owner_key, next_due_date);

CREATE TABLE IF NOT EXISTS cycle_completions (
    id VARCHAR(36) PRIMARY KEY,
    cycle_id VARCHAR(36) NOT NULL,
    owner_key VARCHAR(80) NOT NULL,
    completed_date DATE NOT NULL,
    previous_last_completed_date DATE NOT NULL,
    previous_next_due_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_cycle_completions_cycle FOREIGN KEY (cycle_id) REFERENCES cycle_items(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_cycle_completions_cycle_date ON cycle_completions (cycle_id, completed_date);

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    owner_key VARCHAR(80) NOT NULL,
    endpoint VARCHAR(2048) NOT NULL UNIQUE,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_owner ON push_subscriptions (owner_key);
