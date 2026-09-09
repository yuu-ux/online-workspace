DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS room_categories;
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
    account_status_id SMALLINT NOT NULL REFERENCES account_statuses(id),
    suspended_until TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE room_categories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE profiles (
    user_id BIGINT PRIMARY KEY REFERENCES users(id),
    icon_url VARCHAR(500),
    bio VARCHAR(500) NOT NULL DEFAULT '',
    work_category_id BIGINT REFERENCES room_categories(id),
    is_public BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO account_statuses (id, code) VALUES (1, 'ACTIVE');
INSERT INTO users (id, name, email, password_hash, account_status_id)
VALUES (10, '自分', 'me@example.com', 'password-hash', 1);
