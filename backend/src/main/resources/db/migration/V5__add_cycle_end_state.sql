ALTER TABLE cycle_items ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_cycle_items_owner_end_due
    ON cycle_items (owner_key, ended_at, next_due_date);
