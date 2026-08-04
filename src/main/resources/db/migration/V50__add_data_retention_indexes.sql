CREATE INDEX idx_retention_messages_sent_at
    ON messages (sent_at);

CREATE INDEX idx_retention_users_deleted_at
    ON users (deleted_at)
    WHERE deleted_at IS NOT NULL;

CREATE INDEX idx_retention_work_sessions_user_id
    ON work_sessions (user_id);
