DROP ALL OBJECTS;

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

CREATE TABLE profiles (
    user_id BIGINT PRIMARY KEY,
    icon_url VARCHAR(500),
    bio VARCHAR(500),
    work_category_id SMALLINT,
    is_public BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE room_categories (
    id SMALLINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE friend_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE friends (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_user_id BIGINT NOT NULL,
    status_id SMALLINT NOT NULL
);

INSERT INTO account_statuses (id, code) VALUES
    (1, 'ACTIVE'),
    (2, 'SUSPENDED'),
    (3, 'BANNED');

INSERT INTO room_categories (id, name, description, sort_order) VALUES
    (10, '開発', 'アプリ開発', 1);

INSERT INTO friend_statuses (id, code) VALUES
    (1, 'ACTIVE'),
    (2, 'REMOVED');

INSERT INTO users (id, name, email, password_hash, account_status_id, suspended_until, deleted_at) VALUES
    (1, 'Tom', 'tom@example.com', 'x', 1, NULL, NULL),
    (2, 'Tomoko', 'tomoko@example.com', 'x', 1, NULL, NULL),
    (3, 'Alice', 'alice@example.com', 'x', 1, NULL, NULL),
    (4, 'Tomas', 'suspended@example.com', 'x', 2, TIMESTAMP WITH TIME ZONE '2099-01-01 00:00:00+00', NULL),
    (5, 'TomDeleted', 'deleted@example.com', 'x', 1, NULL, TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00'),
    (6, 'Tomoaki', 'tomoaki@example.com', 'x', 1, NULL, NULL),
    (7, 'Hina', 'hina@example.com', 'x', 1, NULL, NULL);

INSERT INTO profiles (user_id, icon_url, bio, work_category_id, is_public) VALUES
    (1, 'https://example.com/tom.png', 'tom bio', 10, TRUE),
    (2, 'https://example.com/tomoko.png', 'tomoko bio', 10, FALSE),
    (3, NULL, 'alice bio', NULL, TRUE);

INSERT INTO friends (id, user_id, friend_user_id, status_id) VALUES
    (1, 1, 3, 1),
    (2, 1, 2, 2),
    (3, 3, 2, 1);
