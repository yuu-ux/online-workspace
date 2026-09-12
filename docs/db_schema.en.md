# DB Schema

## Design Principles

- Store timestamps with time zone information.
- Use foreign key constraints and guarantee referential integrity in the database as well.
- Manage fixed choices in master tables and reference them by ID from business tables.
- Update `updated_at` in the application or with a database trigger.
- Include `DEFAULT NULL` to explicitly indicate that a value is undecided until it is set.

## Master Tables for Fixed Choices

Manage statuses and other fixed choices with display names and descriptions in addition to string `CHECK` constraints. Each master table has the following common columns.

| Column | Type | Options | Description |
|:--|:--|:--|:--|
| id | SMALLINT | PRIMARY KEY | Fixed ID referenced by business tables |
| code | VARCHAR(50) | NOT NULL, UNIQUE | An identifier used by the application that does not change |
| name | VARCHAR(100) | NOT NULL | Name displayed on screens |
| description | VARCHAR(500) | NOT NULL, DEFAULT '' | Description of the choice |

The initial data is as follows. IDs are shared between environments so that they can be referenced from default values.

| Table | ID | code | name |
|:--|--:|:--|:--|
| account_statuses | 1 | `ACTIVE` | Active |
| account_statuses | 2 | `SUSPENDED` | Suspended |
| account_statuses | 3 | `BANNED` | Permanently banned |
| work_styles | 1 | `FOCUS` | Focus quietly |
| work_styles | 2 | `CHAT_OK` | Chat is welcome |
| room_statuses | 1 | `OPEN` | Open |
| room_statuses | 2 | `CLOSED` | Closed |
| room_category_statuses | 1 | `ACTIVE` | Active |
| room_category_statuses | 2 | `INACTIVE` | Inactive |
| friend_statuses | 1 | `ACTIVE` | Friend |
| friend_statuses | 2 | `REMOVED` | Removed |

## `users` Table

Manages login information and the account's usage status.

| Column | Type | Options | Description |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | User ID |
| name | VARCHAR(100) | NOT NULL | Display name |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email address used for login |
| password_hash | VARCHAR(255) | NOT NULL | Password hashed with bcrypt |
| account_status_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES account_statuses(id) | Account usage status. The default is `ACTIVE`. |
| suspended_until | TIMESTAMPTZ | DEFAULT NULL | End date and time of suspension. Stores a timestamp rather than a day of the week; `NULL` for an indefinite suspension or when not suspended. |
| deleted_at | TIMESTAMPTZ | DEFAULT NULL | Account withdrawal date and time. `NULL` when the account has not been withdrawn. |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Update timestamp |

### Determining Whether an Account May Be Used

Access during authentication is permitted only when all of the following are true: `deleted_at IS NULL`, `account_status_id = ACTIVE`, and `suspended_until IS NULL OR suspended_until <= CURRENT_TIMESTAMP`. Access is denied when `suspended_until` is in the future or `deleted_at` is set, even if the account status is `ACTIVE`.

The combinations that normally represent consistent states are `ACTIVE` with `suspended_until IS NULL` for an active account, `SUSPENDED` with a future `suspended_until` for a temporary suspension, and `BANNED` with `suspended_until IS NULL` for a permanent suspension. A record that reaches the end of its suspension is treated as a transitional state, and the application updates `account_status_id` to `ACTIVE` and `suspended_until` to `NULL` in the same transaction during authentication or periodic processing. Access is not permitted until the update is complete. `deleted_at` is independent of these states and, when set, always means that the account has been withdrawn.

## `profiles` Table

Manages user-published profile information separately from authentication information. Profiles do not have their own status; the account usage status is determined by `users.account_status_id`.

| Column | Type | Options | Description |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | Profile ID |
| user_id | BIGINT | NOT NULL, UNIQUE, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | User ID |
| icon_url | VARCHAR(500) | DEFAULT NULL | URL of the avatar image |
| bio | VARCHAR(500) | NOT NULL, DEFAULT '' | Bio |
| work_category_id | BIGINT | DEFAULT NULL, FOREIGN KEY REFERENCES room_categories(id) | Main work category |
| is_public | BOOLEAN | NOT NULL, DEFAULT TRUE | Whether to make the profile public to other users |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Update timestamp |

## `rooms` Table

Manages the work rooms created by users and their participation conditions.

| Column | Type | Options | Description |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | Room ID |
| name | VARCHAR(100) | NOT NULL | Room name |
| description | VARCHAR(500) | NOT NULL, DEFAULT '' | Room description |
| created_by | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) | User ID of the creator |
| category_id | BIGINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES room_categories(id) | Work category ID. The default is `Uncategorized`. |
| work_style_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES work_styles(id) | Work style. Select from the master table rather than entering free text; the default is `FOCUS`. |
| max_members | SMALLINT | NOT NULL, DEFAULT 12, CHECK (max_members BETWEEN 2 AND 12) | Maximum number of participants |
| status_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES room_statuses(id) | Whether the room is accepting participants or closed. The default is `OPEN`. |
| closed_at | TIMESTAMPTZ | DEFAULT NULL | Date and time when the room was closed. `NULL` while accepting participants. |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Update timestamp |

## `room_categories` Table

Manages master data for work categories prepared by the operations team. The same category record is referenced by multiple rooms and profiles.

Initially register a record with `id = 1` and the name `Uncategorized`. Use this record in existing room creation processes where a category cannot yet be specified.

| Column | Type | Options | Description |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | Category ID |
| name | VARCHAR(100) | NOT NULL, UNIQUE | Category name |
| description | VARCHAR(500) | NOT NULL, DEFAULT '' | Category description |
| status_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES room_category_statuses(id) | Category usage status. The default is `ACTIVE`. |
| sort_order | INT | NOT NULL, DEFAULT 0 | Order in which choices are displayed, in ascending order |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Update timestamp |

## `room_members` Table

Manages users' room participation history. When a user rejoins the same room after leaving, create a new record.

| Column | Type | Options | Description |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | Participation history ID |
| room_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES rooms(id) ON DELETE CASCADE | ID of the room joined |
| user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | ID of the user who joined |
| joined_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Join timestamp |
| left_at | TIMESTAMPTZ | DEFAULT NULL | Leave timestamp. `NULL` while participating. |

### Indexes and Constraints

- `CHECK (left_at IS NULL OR left_at >= joined_at)`
  - Prevents the leave timestamp from being earlier than the join timestamp.
- `UNIQUE INDEX (room_id, user_id) WHERE left_at IS NULL`
  - Prevents the same user from joining the same room more than once at the same time.

### Joining a Room

During the join process, lock the target `rooms` row with `SELECT ... FOR UPDATE` in the same transaction and confirm that `status_id = OPEN`. Then count the records in `room_members` whose `room_id` matches and whose `left_at IS NULL`, and insert a participation history record only when the count is less than `rooms.max_members`. Serializing join processes for the same room with a row lock prevents the capacity from being exceeded by concurrent joins, which cannot be prevented by using a transaction alone.

## `messages` Table

Manages chat messages within rooms.

The current requirements do not provide a way to cancel or delete sent messages. If this is added, consider soft deletion using `deleted_at` rather than physical deletion.

| Column | Type | Options | Description |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | Message ID |
| room_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES rooms(id) ON DELETE CASCADE | ID of the destination room |
| user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) | User ID of the sender |
| content | VARCHAR(500) | NOT NULL | Message body |
| sent_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Sent timestamp |

### Indexes

- `INDEX (room_id, sent_at)`
  - Retrieves messages for each room in sent-time order.

## `friends` Table

Manages friends registered by users in one direction and their removal status. When a friend is removed, do not delete the record; update its status to `REMOVED`.

| Column | Type | Options | Description |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | Friend registration ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | User ID that registered the friend |
| friend_user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | User ID that was registered |
| status_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES friend_statuses(id) | Friend status. The default is `ACTIVE`. |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Update timestamp |

### Indexes and Constraints

- `UNIQUE INDEX (user_id, friend_user_id)`
  - Prevents duplicate registration of the same person and is also used to search the friend list by `user_id`.
- `CHECK (user_id <> friend_user_id)`
  - Prevents registering oneself as a friend.
