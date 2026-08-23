DROP TABLE IF EXISTS blocks;
DROP TABLE IF EXISTS room_members;
DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS room_categories;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS account_statuses;
DROP TABLE IF EXISTS room_statuses;
DROP TABLE IF EXISTS visibilities;
DROP TABLE IF EXISTS work_styles;

CREATE TABLE work_styles (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE visibilities (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE room_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE account_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    account_status_id SMALLINT NOT NULL,
    suspended_until TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);
CREATE TABLE room_categories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL
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
    icon_url VARCHAR(500)
);
CREATE TABLE room_members (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE
);
CREATE TABLE blocks (
    blocker_user_id BIGINT NOT NULL,
    blocked_user_id BIGINT NOT NULL,
    PRIMARY KEY (blocker_user_id, blocked_user_id)
);

INSERT INTO work_styles VALUES (1, 'FOCUS'), (2, 'CHAT_OK');
INSERT INTO visibilities VALUES (1, 'PUBLIC'), (2, 'INVITE_ONLY');
INSERT INTO room_statuses VALUES (1, 'OPEN'), (2, 'CLOSED');
INSERT INTO account_statuses VALUES (1, 'ACTIVE'), (2, 'SUSPENDED'), (3, 'BANNED');
INSERT INTO users VALUES
    (1, '閲覧者', 'viewer@example.com', 1, NULL, NULL),
    (2, '作成者', 'creator@example.com', 1, NULL, NULL),
    (3, 'ブロック相手', 'blocked@example.com', 1, NULL, NULL),
    (4, '参加者', 'member@example.com', 1, NULL, NULL),
    (5, '閲覧者がブロックした相手', 'blocked-by-viewer@example.com', 1, NULL, NULL),
    (6, '停止中', 'suspended@example.com', 2, TIMESTAMP WITH TIME ZONE '2099-01-01 00:00:00+00', NULL);
INSERT INTO room_categories VALUES
    (1, '開発', 'ソフトウェア開発', 10),
    (2, '読書', '読書会', 20);
INSERT INTO profiles VALUES (2, 'https://example.com/icon.png');
INSERT INTO rooms VALUES
    (10, '参加可能', 2, 1, 1, 3, 1, 1, TIMESTAMP WITH TIME ZONE '2026-08-10 10:00:00+09:00'),
    (11, '招待限定', 2, 1, 1, 3, 2, 1, TIMESTAMP WITH TIME ZONE '2026-08-11 10:00:00+09:00'),
    (12, '終了済み', 2, 1, 1, 3, 1, 2, TIMESTAMP WITH TIME ZONE '2026-08-12 10:00:00+09:00'),
    (13, 'ブロック中', 2, 1, 2, 3, 1, 1, TIMESTAMP WITH TIME ZONE '2026-08-13 10:00:00+09:00'),
    (14, '満室', 2, 1, 1, 2, 1, 1, TIMESTAMP WITH TIME ZONE '2026-08-14 10:00:00+09:00'),
    (15, '閲覧者がブロック中', 5, 2, 1, 3, 1, 1, TIMESTAMP WITH TIME ZONE '2026-08-09 10:00:00+09:00');
INSERT INTO room_members VALUES
    (100, 10, 2, NULL),
    (101, 13, 2, NULL),
    (102, 13, 3, NULL),
    (103, 14, 2, NULL),
    (104, 14, 4, NULL),
    (105, 15, 5, NULL);
INSERT INTO blocks VALUES (3, 1), (1, 5);
