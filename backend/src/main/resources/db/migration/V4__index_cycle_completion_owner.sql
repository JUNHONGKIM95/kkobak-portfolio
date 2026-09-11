CREATE INDEX IF NOT EXISTS idx_cycle_completions_owner_date
    ON cycle_completions (owner_key, completed_date);
