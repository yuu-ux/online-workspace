DROP INDEX IF EXISTS idx_users_withdrawal_purge;

ALTER TABLE users
    DROP COLUMN IF EXISTS personal_data_purged_at;
