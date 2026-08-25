DROP INDEX idx_rooms_public_list;

CREATE INDEX idx_rooms_open_list
    ON rooms (status_id, created_at DESC, id DESC);

ALTER TABLE rooms DROP COLUMN visibility_id;

DROP TABLE visibilities;
DROP TABLE blocks;

DROP TABLE admin_actions;
DROP FUNCTION validate_admin_action_period();
DROP TABLE admin_action_types;

ALTER TABLE reports DROP COLUMN status_id;

DROP TABLE report_statuses;

ALTER TABLE users DROP COLUMN role_id;

DROP TABLE roles;
