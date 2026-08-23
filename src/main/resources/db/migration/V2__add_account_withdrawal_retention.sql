ALTER TABLE users
    ADD COLUMN personal_data_purged_at TIMESTAMPTZ DEFAULT NULL;

CREATE INDEX idx_users_withdrawal_purge
    ON users (deleted_at)
    WHERE deleted_at IS NOT NULL AND personal_data_purged_at IS NULL;
