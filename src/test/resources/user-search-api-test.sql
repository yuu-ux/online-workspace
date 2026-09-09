DROP TABLE IF EXISTS profiles;
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

CREATE TABLE profiles (
    user_id BIGINT PRIMARY KEY,
    icon_url VARCHAR(500)
);

INSERT INTO account_statuses (id, code) VALUES
    (1, 'ACTIVE'),
    (2, 'SUSPENDED'),
    (3, 'BANNED');

INSERT INTO users (id, name, email, password_hash, account_status_id, suspended_until, deleted_at) VALUES
    (1, 'Tom', 'tom@example.com', 'x', 1, NULL, NULL),
    (2, 'Tomoko', 'tomoko@example.com', 'x', 1, NULL, NULL),
    (3, 'Alice', 'alice@example.com', 'x', 1, NULL, NULL),
    (4, 'Tomas', 'suspended@example.com', 'x', 2, TIMESTAMP WITH TIME ZONE '2099-01-01 00:00:00+00', NULL),
    (5, 'TomDeleted', 'deleted@example.com', 'x', 1, NULL, TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00');

INSERT INTO profiles (user_id, icon_url) VALUES
    (1, 'https://example.com/tom.png'),
    (2, 'https://example.com/tomoko.png');
