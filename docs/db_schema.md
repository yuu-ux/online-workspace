# DB スキーマ

## 設計方針

- 日時はタイムゾーン付きで保存する。
- 外部キー制約を使用し、参照整合性をデータベースでも保証する。
- 固定選択肢はマスターテーブルで管理し、業務テーブルからIDで参照する。
- `updated_at` はアプリケーション、またはデータベースのトリガーで更新する。
- `DEFAULT NULL` は、値が設定されるまで未確定であることを明示するために記載する。

## 固定選択肢のマスターテーブル

状態などを文字列の `CHECK` 制約だけで管理せず、表示名や説明も含めて管理する。各マスターテーブルは次の共通カラムを持つ。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | SMALLINT | PRIMARY KEY | 業務テーブルから参照する固定ID |
| code | VARCHAR(50) | NOT NULL, UNIQUE | アプリケーションで使用する変更しない識別子 |
| name | VARCHAR(100) | NOT NULL | 画面に表示する名称 |
| description | VARCHAR(500) | NOT NULL, DEFAULT '' | 選択肢の説明 |

初期データは次のとおりとする。既定値から参照するため、IDは環境間で共通にする。

| テーブル名 | ID | code | name |
|:--|--:|:--|:--|
| account_statuses | 1 | `ACTIVE` | 利用中 |
| account_statuses | 2 | `SUSPENDED` | 一時停止 |
| account_statuses | 3 | `BANNED` | 永久停止 |
| work_styles | 1 | `FOCUS` | 黙って集中 |
| work_styles | 2 | `CHAT_OK` | 雑談OK |
| room_statuses | 1 | `OPEN` | 受付中 |
| room_statuses | 2 | `CLOSED` | 終了 |
| room_category_statuses | 1 | `ACTIVE` | 利用中 |
| room_category_statuses | 2 | `INACTIVE` | 利用停止 |
| friend_statuses | 1 | `ACTIVE` | フレンド |
| friend_statuses | 2 | `REMOVED` | 解除済み |
| report_reasons | 1 | `HARASSMENT` | ハラスメント |
| report_reasons | 2 | `DEFAMATION` | 誹謗中傷 |
| report_reasons | 3 | `SPAM` | スパム |
| report_reasons | 4 | `FRAUD_OR_IMPERSONATION` | 詐欺・なりすまし |
| report_reasons | 5 | `INAPPROPRIATE_CONTENT` | 不適切コンテンツ |
| report_reasons | 6 | `OTHER` | その他 |

## users テーブル

ログイン情報とアカウントの利用状態を管理する。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | ユーザーID |
| name | VARCHAR(100) | NOT NULL | 表示名 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | ログインに使用するメールアドレス |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt でハッシュ化したパスワード |
| account_status_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES account_statuses(id) | アカウントの利用状態。既定値は `ACTIVE` |
| suspended_until | TIMESTAMPTZ | DEFAULT NULL | 一時停止の終了日時。曜日ではなく日時を保存し、無期限停止または停止中でない場合は `NULL` |
| deleted_at | TIMESTAMPTZ | DEFAULT NULL | 退会日時。未退会の場合は `NULL` |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新日時 |

### アカウント利用可否の判定

認証時のアクセス許可は、`deleted_at IS NULL`、`account_status_id = ACTIVE`、かつ `suspended_until IS NULL OR suspended_until <= CURRENT_TIMESTAMP` のすべてを満たす場合に限る。`ACTIVE` であっても `suspended_until` が未来の場合や、`deleted_at` が設定されている場合はアクセスを拒否する。

通常時に整合する状態の組み合わせは、利用中が `ACTIVE` と `suspended_until IS NULL`、一時停止中が `SUSPENDED` と未来の `suspended_until`、永久停止が `BANNED` と `suspended_until IS NULL` とする。一時停止の期限に到達したレコードは移行状態として扱い、アプリケーションが認証処理または定期処理で `account_status_id` を `ACTIVE`、`suspended_until` を `NULL` へ同一トランザクション内で更新する。更新が完了するまではアクセスを許可しない。`deleted_at` はこれらの状態と独立しており、値がある場合は常に退会済みとして扱う。

## profiles テーブル

ユーザーが公開するプロフィール情報を、認証情報と分離して管理する。プロフィール独自の状態は持たず、アカウントの利用状態は `users.account_status_id` を参照する。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | プロフィールID |
| user_id | BIGINT | NOT NULL, UNIQUE, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | ユーザーID |
| icon_url | VARCHAR(500) | DEFAULT NULL | アイコン画像のURL |
| bio | VARCHAR(500) | NOT NULL, DEFAULT '' | 自己紹介 |
| work_category_id | BIGINT | DEFAULT NULL, FOREIGN KEY REFERENCES room_categories(id) | 主な作業カテゴリ |
| is_public | BOOLEAN | NOT NULL, DEFAULT TRUE | プロフィールを他ユーザーへ公開するか |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新日時 |

## rooms テーブル

ユーザーが作成する作業ルームと、その参加条件を管理する。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | ルームID |
| name | VARCHAR(100) | NOT NULL | ルーム名 |
| description | VARCHAR(500) | NOT NULL, DEFAULT '' | ルームの説明 |
| created_by | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) | 作成者のユーザーID |
| category_id | BIGINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES room_categories(id) | 作業カテゴリID。既定値は `未分類` |
| work_style_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES work_styles(id) | 作業スタイル。自由入力ではなくマスターから選択し、既定値は `FOCUS` |
| max_members | SMALLINT | NOT NULL, DEFAULT 12, CHECK (max_members BETWEEN 2 AND 12) | 参加人数の上限 |
| status_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES room_statuses(id) | ルームが参加受付中か、閉じられているか。既定値は `OPEN` |
| closed_at | TIMESTAMPTZ | DEFAULT NULL | ルームを閉じた日時。受付中の場合は `NULL` |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新日時 |

## room_categories テーブル

運営が用意する作業カテゴリのマスターデータを管理する。同じカテゴリレコードを複数のルームやプロフィールから参照する。

初期データとして `id = 1`、名称 `未分類` のレコードを登録し、カテゴリをまだ指定できない既存のルーム作成処理ではこのレコードを使用する。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | カテゴリID |
| name | VARCHAR(100) | NOT NULL, UNIQUE | カテゴリ名 |
| description | VARCHAR(500) | NOT NULL, DEFAULT '' | カテゴリの説明 |
| status_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES room_category_statuses(id) | カテゴリの利用状態。既定値は `ACTIVE` |
| sort_order | INT | NOT NULL, DEFAULT 0 | 選択肢を表示する順番。昇順で表示する |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新日時 |

## room_members テーブル

ユーザーのルーム参加履歴を管理する。退出後に同じルームへ再参加した場合は、新しいレコードを作成する。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | 参加履歴ID |
| room_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES rooms(id) ON DELETE CASCADE | 参加したルームID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | 参加したユーザーID |
| joined_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 入室日時 |
| left_at | TIMESTAMPTZ | DEFAULT NULL | 退出日時。参加中の場合は `NULL` |

### インデックス・制約

- `CHECK (left_at IS NULL OR left_at >= joined_at)`
  - 退出日時が入室日時より前になることを防ぐ。
- `UNIQUE INDEX (room_id, user_id) WHERE left_at IS NULL`
  - 同じユーザーが同じルームへ重複参加することを防ぐ。

### 参加処理

参加処理では、同一トランザクション内で対象の `rooms` 行を `SELECT ... FOR UPDATE` によりロックし、`status_id = OPEN` であることを確認する。続けて `room_members` の `room_id` が一致し `left_at IS NULL` であるレコードを数え、その件数が `rooms.max_members` 未満の場合に限り参加履歴を `INSERT` する。同じルームへの参加処理を行ロックで直列化することで、単にトランザクションを使用するだけでは防げない同時参加による上限超過を防ぐ。

## messages テーブル

ルーム内チャットのメッセージを管理する。

現要件では送信済みメッセージの取消し・削除を提供しない。追加する場合は、物理削除ではなく `deleted_at` による論理削除を検討する。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | メッセージID |
| room_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES rooms(id) ON DELETE CASCADE | 送信先ルームID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) | 送信者のユーザーID |
| content | VARCHAR(500) | NOT NULL | メッセージ本文 |
| sent_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 送信日時 |

### インデックス

- `INDEX (room_id, sent_at)`
  - ルームごとのメッセージを送信日時順に取得する。

## friends テーブル

ユーザーが一方向に登録したフレンドと、お気に入り・解除状態を管理する。フレンド解除時はレコードを削除せず、状態を `REMOVED` に更新する。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | フレンド登録ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | 登録したユーザーID |
| friend_user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | 登録されたユーザーID |
| is_favorite | BOOLEAN | NOT NULL, DEFAULT FALSE | お気に入りに登録しているか |
| status_id | SMALLINT | NOT NULL, DEFAULT 1, FOREIGN KEY REFERENCES friend_statuses(id) | フレンド状態。既定値は `ACTIVE` |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新日時 |

### インデックス・制約

- `UNIQUE INDEX (user_id, friend_user_id)`
  - 同じ相手の重複登録を防ぎ、`user_id` を使うフレンド一覧検索にも利用する。
- `CHECK (user_id <> friend_user_id)`
  - 自分自身のフレンド登録を防ぐ。

## reports テーブル

ユーザーに対する通報と、その確認状況を管理する。現要件では通報対象はメッセージ単位ではなくユーザー単位とする。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | 通報ID |
| reporter_user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) | 通報したユーザーID |
| target_user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) | 通報対象のユーザーID |
| room_id | BIGINT | DEFAULT NULL, FOREIGN KEY REFERENCES rooms(id) | 問題が起きたルームID |
| reason_id | SMALLINT | NOT NULL, FOREIGN KEY REFERENCES report_reasons(id) | 選択式の通報理由。自由入力ではなくマスターから選択する |
| details | TEXT | DEFAULT NULL | 通報者が任意入力する詳細説明 |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 通報日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新日時 |

### 制約

- `CHECK (reporter_user_id <> target_user_id)`
  - 自分自身への通報を防ぐ。

## work_sessions テーブル

ユーザーがルームに入室してから退出するまでの自動計測結果を、作業履歴として管理する。任意の手動タイマーそのものを管理するテーブルではない。

| カラム名 | 型 | オプション | 説明 |
|:--|:--|:--|:--|
| id | BIGSERIAL | PRIMARY KEY | 作業履歴ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE | 作業したユーザーID |
| room_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES rooms(id) | 作業したルームID |
| category_id | BIGINT | NOT NULL, FOREIGN KEY REFERENCES room_categories(id) | 作業時点のカテゴリID |
| started_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 入室して自動計測を開始した日時 |
| ended_at | TIMESTAMPTZ | DEFAULT NULL | 退出して自動計測を終了した日時。計測中は `NULL` |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新日時 |

### インデックス・制約

- `CHECK (ended_at IS NULL OR ended_at >= started_at)`
  - 終了日時が開始日時より前になることを防ぐ。
- `UNIQUE INDEX (user_id) WHERE ended_at IS NULL`
  - 1人のユーザーが複数のルームで同時に計測を開始することを防ぐ。
