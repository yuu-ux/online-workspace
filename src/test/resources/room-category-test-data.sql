DROP TABLE IF EXISTS room_categories;
DROP TABLE IF EXISTS room_category_statuses;

CREATE TABLE room_category_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE room_categories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status_id SMALLINT NOT NULL,
    sort_order INT NOT NULL
);

INSERT INTO room_category_statuses VALUES
    (1, 'ACTIVE'),
    (2, 'INACTIVE');
INSERT INTO room_categories VALUES
    (1, '開発', 'ソフトウェア開発', 1, 20),
    (2, '休止中', '表示しない', 2, 1),
    (3, '読書', '本を読む', 1, 10);
