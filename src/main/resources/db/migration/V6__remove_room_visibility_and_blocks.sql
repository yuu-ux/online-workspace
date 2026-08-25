DROP INDEX idx_rooms_public_list;

CREATE INDEX idx_rooms_open_list
    ON rooms (status_id, created_at DESC, id DESC);

ALTER TABLE rooms DROP COLUMN visibility_id;

DROP TABLE visibilities;

DROP TABLE blocks;
