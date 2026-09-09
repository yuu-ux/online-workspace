DROP TABLE IF EXISTS room_members;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS account_statuses;

CREATE TABLE account_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    account_status_id SMALLINT NOT NULL,
    suspended_until TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE rooms (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE room_members (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE
);

INSERT INTO account_statuses (id, code) VALUES (1, 'ACTIVE'), (2, 'SUSPENDED');
INSERT INTO users (id, name, email, password_hash, account_status_id, suspended_until, deleted_at) VALUES
    (1, '履歴ユーザー', 'history@example.com', 'x', 1, NULL, NULL),
    (2, '停止中ユーザー', 'suspended@example.com', 'x', 2, TIMESTAMP WITH TIME ZONE '2099-01-01 00:00:00+00', NULL);

INSERT INTO rooms (id, name) VALUES
    (10, '集中ルーム'),
    (11, '雑談ルーム');

INSERT INTO room_members (id, room_id, user_id, joined_at, left_at) VALUES
    (100, 10, 1, TIMESTAMP WITH TIME ZONE '2026-08-01 10:00:00+00', TIMESTAMP WITH TIME ZONE '2026-08-01 11:00:00+00'),
    (101, 11, 1, TIMESTAMP WITH TIME ZONE '2026-08-02 10:00:00+00', NULL);
