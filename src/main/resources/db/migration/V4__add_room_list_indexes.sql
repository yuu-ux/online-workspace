CREATE INDEX idx_rooms_public_list
    ON rooms (status_id, visibility_id, created_at DESC, id DESC);

CREATE INDEX idx_blocks_blocked_blocker
    ON blocks (blocked_user_id, blocker_user_id);
