CREATE TABLE roles (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE account_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE work_styles (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE visibilities (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE room_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE room_category_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE friend_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE report_reasons (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE report_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE admin_action_types (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

INSERT INTO roles (id, code, name) VALUES
    (1, 'USER', '一般ユーザー'),
    (2, 'ADMIN', '管理者');

INSERT INTO account_statuses (id, code, name) VALUES
    (1, 'ACTIVE', '利用中'),
    (2, 'SUSPENDED', '一時停止'),
    (3, 'BANNED', '永久停止');

INSERT INTO work_styles (id, code, name) VALUES
    (1, 'FOCUS', '黙って集中'),
    (2, 'CHAT_OK', '雑談OK');

INSERT INTO visibilities (id, code, name) VALUES
    (1, 'PUBLIC', '公開'),
    (2, 'INVITE_ONLY', '招待のみ'),
    (3, 'FRIENDS_ONLY', 'フレンドのみ');

INSERT INTO room_statuses (id, code, name) VALUES
    (1, 'OPEN', '受付中'),
    (2, 'CLOSED', '終了');

INSERT INTO room_category_statuses (id, code, name) VALUES
    (1, 'ACTIVE', '利用中'),
    (2, 'INACTIVE', '利用停止');

INSERT INTO friend_statuses (id, code, name) VALUES
    (1, 'ACTIVE', 'フレンド'),
    (2, 'REMOVED', '解除済み');

INSERT INTO report_reasons (id, code, name) VALUES
    (1, 'HARASSMENT', 'ハラスメント'),
    (2, 'DEFAMATION', '誹謗中傷'),
    (3, 'SPAM', 'スパム'),
    (4, 'FRAUD_OR_IMPERSONATION', '詐欺・なりすまし'),
    (5, 'INAPPROPRIATE_CONTENT', '不適切コンテンツ'),
    (6, 'OTHER', 'その他');

INSERT INTO report_statuses (id, code, name) VALUES
    (1, 'PENDING', '未確認'),
    (2, 'REVIEWING', '確認中'),
    (3, 'RESOLVED', '対応済み'),
    (4, 'DISMISSED', '対応不要');

INSERT INTO admin_action_types (id, code, name) VALUES
    (1, 'WARNING', '警告'),
    (2, 'TEMPORARY_SUSPENSION', '一時停止'),
    (3, 'PERMANENT_SUSPENSION', '永久停止');

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id SMALLINT NOT NULL DEFAULT 1 REFERENCES roles(id),
    account_status_id SMALLINT NOT NULL DEFAULT 1 REFERENCES account_statuses(id),
    suspended_until TIMESTAMPTZ DEFAULT NULL,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE room_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) NOT NULL DEFAULT '',
    status_id SMALLINT NOT NULL DEFAULT 1 REFERENCES room_category_statuses(id),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO room_categories (id, name, description) VALUES
    (1, '未分類', 'ルーム作成時にカテゴリが指定されなかった場合の既定カテゴリ');

SELECT setval(pg_get_serial_sequence('room_categories', 'id'), 1, true);

CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    icon_url VARCHAR(500) DEFAULT NULL,
    bio VARCHAR(500) NOT NULL DEFAULT '',
    work_category_id BIGINT DEFAULT NULL REFERENCES room_categories(id),
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    created_by BIGINT NOT NULL REFERENCES users(id),
    category_id BIGINT NOT NULL DEFAULT 1 REFERENCES room_categories(id),
    work_style_id SMALLINT NOT NULL DEFAULT 1 REFERENCES work_styles(id),
    max_members SMALLINT NOT NULL DEFAULT 12 CHECK (max_members BETWEEN 2 AND 12),
    visibility_id SMALLINT NOT NULL DEFAULT 1 REFERENCES visibilities(id),
    status_id SMALLINT NOT NULL DEFAULT 1 REFERENCES room_statuses(id),
    closed_at TIMESTAMPTZ DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE room_members (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT chk_room_members_period CHECK (left_at IS NULL OR left_at >= joined_at)
);

CREATE UNIQUE INDEX uq_room_members_active
    ON room_members (room_id, user_id)
    WHERE left_at IS NULL;

CREATE TABLE room_invites (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 hour'),
    invalidated_at TIMESTAMPTZ DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    content VARCHAR(500) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_room_sent_at ON messages (room_id, sent_at);

CREATE TABLE friends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    status_id SMALLINT NOT NULL DEFAULT 1 REFERENCES friend_statuses(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_friends_not_self CHECK (user_id <> friend_user_id)
);

CREATE UNIQUE INDEX uq_friends_user_friend ON friends (user_id, friend_user_id);

CREATE TABLE blocks (
    blocker_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blocker_user_id, blocked_user_id),
    CONSTRAINT chk_blocks_not_self CHECK (blocker_user_id <> blocked_user_id)
);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_user_id BIGINT NOT NULL REFERENCES users(id),
    target_user_id BIGINT NOT NULL REFERENCES users(id),
    room_id BIGINT DEFAULT NULL REFERENCES rooms(id),
    reason_id SMALLINT NOT NULL REFERENCES report_reasons(id),
    details TEXT DEFAULT NULL,
    status_id SMALLINT NOT NULL DEFAULT 1 REFERENCES report_statuses(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reports_not_self CHECK (reporter_user_id <> target_user_id)
);

CREATE TABLE admin_actions (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT DEFAULT NULL REFERENCES reports(id),
    admin_user_id BIGINT NOT NULL REFERENCES users(id),
    target_user_id BIGINT NOT NULL REFERENCES users(id),
    action_type_id SMALLINT NOT NULL REFERENCES admin_action_types(id),
    reason TEXT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMPTZ DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_admin_actions_period CHECK (ends_at IS NULL OR ends_at > starts_at)
);

CREATE FUNCTION validate_admin_action_period()
RETURNS TRIGGER AS $$
DECLARE
    action_code VARCHAR(50);
BEGIN
    SELECT code INTO action_code
    FROM admin_action_types
    WHERE id = NEW.action_type_id;

    IF action_code = 'TEMPORARY_SUSPENSION' THEN
        IF NEW.ends_at IS NULL OR NEW.ends_at <= NEW.starts_at THEN
            RAISE EXCEPTION 'TEMPORARY_SUSPENSION requires ends_at after starts_at';
        END IF;
    ELSIF action_code IN ('WARNING', 'PERMANENT_SUSPENSION') AND NEW.ends_at IS NOT NULL THEN
        RAISE EXCEPTION '% requires ends_at to be NULL', action_code;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_admin_action_period
    BEFORE INSERT OR UPDATE OF action_type_id, starts_at, ends_at
    ON admin_actions
    FOR EACH ROW
    EXECUTE FUNCTION validate_admin_action_period();

CREATE TABLE work_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    category_id BIGINT NOT NULL REFERENCES room_categories(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_work_sessions_period CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE UNIQUE INDEX uq_work_sessions_active_user
    ON work_sessions (user_id)
    WHERE ended_at IS NULL;
