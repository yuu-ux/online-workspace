DROP TABLE IF EXISTS work_sessions;
DROP TABLE IF EXISTS room_members;
DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS room_categories;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS work_styles;
DROP TABLE IF EXISTS visibilities;
DROP TABLE IF EXISTS room_statuses;

CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE room_categories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE work_styles (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL
);

CREATE TABLE visibilities (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL
);

CREATE TABLE room_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL
);

CREATE TABLE rooms (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_by BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    work_style_id SMALLINT NOT NULL,
    max_members SMALLINT NOT NULL,
    visibility_id SMALLINT NOT NULL,
    status_id SMALLINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE profiles (
    user_id BIGINT PRIMARY KEY,
    icon_url VARCHAR(500) DEFAULT NULL
);

CREATE TABLE room_members (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE work_sessions (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

INSERT INTO users (id, name, email) VALUES
    (10, '自分', 'me@example.com'),
    (11, '共同作業者', 'peer@example.com'),
    (12, 'ルーム作成者', 'owner@example.com');
INSERT INTO profiles (user_id, icon_url) VALUES
    (11, 'https://example.com/peer.png');
INSERT INTO room_categories (id, name, description, sort_order) VALUES
    (30, '開発', 'プログラミング', 1),
    (31, '読書', '本を読む', 2);
INSERT INTO work_styles (id, code) VALUES (1, 'FOCUS');
INSERT INTO visibilities (id, code) VALUES (1, 'PUBLIC');
INSERT INTO room_statuses (id, code) VALUES (1, 'OPEN');
INSERT INTO rooms (
    id, name, created_by, category_id, work_style_id, max_members,
    visibility_id, status_id, created_at
) VALUES (
    20, '朝活ルーム', 12, 30, 1, 8, 1, 1, TIMESTAMP WITH TIME ZONE '2026-08-01 00:00:00+00'
);
INSERT INTO room_members (id, room_id, user_id, joined_at, left_at) VALUES
    (40, 20, 10, TIMESTAMP WITH TIME ZONE '2026-08-03 01:00:00+00', TIMESTAMP WITH TIME ZONE '2026-08-03 02:30:00+00'),
    (41, 20, 11, TIMESTAMP WITH TIME ZONE '2026-08-03 01:30:00+00', TIMESTAMP WITH TIME ZONE '2026-08-03 02:00:00+00');
INSERT INTO work_sessions (id, user_id, room_id, category_id, started_at, ended_at) VALUES
    (50, 10, 20, 30, TIMESTAMP WITH TIME ZONE '2026-08-03 01:00:00+00', TIMESTAMP WITH TIME ZONE '2026-08-03 02:30:00+00');
