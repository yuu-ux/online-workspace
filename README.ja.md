_このプロジェクトは、42のカリキュラムの一環として yehara、tmuranak、kofujita、yonuma によって作成されました。_

# Online Workspace

## 概要

Online Workspace は、オンラインで他のユーザーと一緒に作業するためのWebアプリケーションです。ユーザーは作業ルームを作成・検索・参加でき、参加者の在席状況を確認しながらリアルタイムチャットでコミュニケーションできます。

安全なREST API、WebSocketによるリアルタイム通信、監視可能なコンテナ環境を重視しています。

### 主な機能

- ユーザー登録、ログイン、ログアウト、セッション管理、退会
- プロフィール編集、アイコン画像アップロード、ユーザー検索、公開設定
- ルームの作成、一覧、絞り込み、ページネーション、参加、退出、更新、終了
- ルームカテゴリ、作業スタイル、人数上限、参加者の在席状態
- 保存されたチャット履歴とWebSocketによるリアルタイム配信
- フレンド管理とフレンドのオンライン状態通知
- CSRF対策、API key認証、レート制限、セキュリティ監査ログ
- Prometheusメトリクス、Grafanaダッシュボード、アラート、ELKによるログ収集

## 実行手順

### 前提条件

| ツール | 要件 |
| --- | --- |
| Docker | Docker Engine と Docker Compose |
| Java | バックエンドをDocker外で実行する場合はJDK 25 |
| Node.js / npm | フロントエンドをDocker外で開発する場合に必要 |
| Gleam | フロントエンドをDocker外で開発する場合に必要 |

標準のDocker Compose構成では、フロントエンド、バックエンド、PostgreSQL、リバースプロキシを起動します。

### 開発環境の起動

```bash
docker compose up --build
```

開発用プロキシには次のURLからアクセスできます。

- HTTPSアプリケーション: `https://localhost:8443`
- HTTPリダイレクト用エンドポイント: `http://localhost:8088`
- バックエンドへの直接アクセス: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

プロキシでは開発用の自己署名証明書を使用します。起動時にブラウザで証明書の例外設定が必要になる場合があります。

### バックエンド単体の起動

先にPostgreSQLを起動してから、次を実行します。

```bash
SESSION_COOKIE_SECURE=false ./gradlew bootRun
```

環境変数が未設定の場合は、次の値が使用されます。

| 変数 | デフォルト値 |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `password` |
| `MAIL_HOST` | `localhost` |
| `MAIL_PORT` | `1025` |
| `SESSION_COOKIE_HTTP_ONLY` | `true` |
| `SESSION_COOKIE_SECURE` | `true` |
| `SESSION_COOKIE_SAME_SITE` | `lax` |

HTTPで直接アクセスする場合は `SESSION_COOKIE_SECURE=false` を設定してください。HTTPS経由では `true` を使用します。

### 監視環境の起動

監視サービスは `compose.observability.yaml` に定義されています。必要な変数を設定して次を実行します。

```bash
MANAGEMENT_API_KEY="change-me" \
GRAFANA_ADMIN_PASSWORD="change-me" \
GRAFANA_ALERT_DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..." \
docker compose -f compose.yaml -f compose.observability.yaml up prometheus grafana
```

利用できる主なサービスは次のとおりです。

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

### テスト

```bash
./gradlew test
```

REST APIの契約は次で検証できます。

```bash
npx --yes @redocly/cli@2.43.2 lint online-workspace@v1
```

## 参考資料

### プロジェクト内ドキュメント

- [要件定義](docs/requirements.md)
- [API契約](docs/openapi.yaml)
- [API開発手順](docs/api_development.md)
- [WebSocketイベント契約](docs/websocket_events.md)
- [DBスキーマ](docs/db_schema.md)
- [Webセキュリティ方針](docs/web_security.md)
- [退会時のデータ保持方針](docs/account_withdrawal_data_policy.md)
- [アーキテクチャ](docs/architecture.md)
- [42 subject](en.subject.pdf)

### 外部参考資料

- [ft_transcendence READMEの例](https://github.com/team-cinnamoroll/ft_transcendence)
- [42 Eval Hub: ft_transcendence](https://www.42evalhub.com/common/fttranscendence)
- [Spring Boot ドキュメント](https://spring.io/projects/spring-boot)
- [Gleam ドキュメント](https://gleam.run/documentation/)
- [PostgreSQL ドキュメント](https://www.postgresql.org/docs/)
- [Prometheus ドキュメント](https://prometheus.io/docs/)
- [Grafana ドキュメント](https://grafana.com/docs/)

### AIの利用

TODO: 使用したAIツール、用途、生成物をチームでどのようにレビュー・テストしたかを記載してください。

## チーム情報

チームは4名で構成されています。役割と担当はチームで記入するため、ここでは TODO としています。

| メンバー | 役割 | 担当 |
| --- | --- | --- |
| `yehara` | TODO | TODO |
| `tmuranak` | TODO | TODO |
| `kofujita` | TODO | TODO |
| `yonuma` | TODO | TODO |

## プロジェクト管理

- コミュニケーション手段: TODO
- 定例ミーティング: TODO
- タスク管理ツール: GitHub Issues、GitHub Projects
- コードレビューの運用: TODO
- ブランチ運用: [ブランチ命名規則](docs/branch_naming_convention.md)

## 技術スタック

### フロントエンド

- Gleam
- Lustre
- Tailwind CSS
- ブラウザAPIとSockJS/STOMP通信のためのJavaScript FFI

型安全な関数型フロントエンドを構築するためにGleamを使用しています。Lustreでアプリケーション構造を作り、Tailwind CSSでスタイリングしています。

### バックエンド

- Java 25
- Spring Boot 4
- Spring Security
- Spring Web MVC
- Spring WebSocket / STOMP
- Bean Validation
- MyBatis
- Gradle

Spring BootでHTTPサーバーとWebSocketサーバーを構築しています。Spring Securityでセッション認証、CSRF対策、API key、アクセス制御、セキュリティイベントを扱い、MyBatisでSQLマッピングとデータベースアクセスを行っています。

### データベースとインフラ

- PostgreSQL 16
- Flyway
- Docker / Docker Compose
- Nginx
- Prometheus / Grafana
- Elasticsearch / Logstash / Kibana

Docker Composeで再現可能な開発環境を構築し、NginxをHTTPSリバースプロキシとして使用しています。

## データベーススキーマ

主なエンティティの関係は次のとおりです。

```text
users
├── user_profiles
├── room_memberships ── rooms ── room_categories
├── chat_messages
├── friendships
└── account status / withdrawal data
```

主なテーブルは次のとおりです。

- `users`: アカウントと認証に関する情報
- `user_profiles`: 名前、プロフィール、作業カテゴリ、公開設定、アイコン情報
- `rooms`: ルーム設定、人数上限、作業スタイル、状態、作成者
- `room_categories`: 管理者が設定するルームカテゴリ
- `room_memberships`: ユーザーとルームの所属関係
- `chat_messages`: ルーム内チャットの履歴
- `friendships`: フレンド関係と解除済み関係

制約を含む詳細なスキーマは [docs/db_schema.md](docs/db_schema.md) に記載しています。データベースの変更はFlywayマイグレーションで適用します。

## 機能一覧

| 機能 | 内容 | 担当者 |
| --- | --- | --- |
| 認証 | 登録、ログイン、ログアウト、セッション、CSRFトークン | TODO |
| プロフィール管理 | 公開設定、自己紹介、作業カテゴリ、アイコン管理 | TODO |
| ユーザー検索 | 名前によるページネーション付き検索と公開プロフィール | TODO |
| ルーム管理 | 作成、一覧、絞り込み、詳細、更新、終了 | TODO |
| ルーム参加 | 参加、退出、参加者一覧、人数制限、アクセス制御 | TODO |
| チャット | ページネーション付き履歴とメッセージ検証 | TODO |
| リアルタイム更新 | チャット、在席状態、参加人数、ルーム作成、フレンド状態 | TODO |
| フレンド管理 | 追加、一覧、解除、オンライン状態 | TODO |
| 公開API | OpenAPIで定義したセッション/API key認証とレート制限付きREST API | TODO |
| 可観測性 | ヘルスチェック、Prometheus、Grafana、アラート、ELKログ | TODO |

## モジュール

現在のモジュール計画は14ポイントです。担当者と評価時の記録は TODO としています。

| モジュール | 区分 | 実装内容 | 担当 |
| --- | ---: | --- | --- |
| フロントエンド・バックエンドフレームワーク | Major, 2点 | Gleam/LustreフロントエンドとSpring Bootバックエンド | TODO |
| リアルタイム機能 | Major, 2点 | チャットと在席状態のSTOMP over SockJS/WebSocket | TODO |
| ユーザー間機能 | Major, 2点 | プロフィール、フレンド、ルーム参加、チャット | TODO |
| 公開API | Major, 2点 | API keyとレート制限を備えたOpenAPI REST API | TODO |
| ログ管理基盤 | Major, 2点 | Elasticsearch、Logstash、Kibana | TODO |
| 監視システム | Major, 2点 | Prometheus、Grafana、ダッシュボード、アラート | TODO |
| ファイルアップロード・管理 | Minor, 1点 | アイコンのアップロード、保存、取得、削除 | TODO |
| ヘルスチェック・ステータス | Minor, 1点 | 保護されたActuatorのhealth / Prometheus endpoint | TODO |
| **合計** | **14点** |  |  |

## 個人の貢献

以下はチームで記入するため、すべて TODO としています。

### `yehara`

- 役割: TODO
- 貢献: TODO
- 課題と解決方法: TODO

### `tmuranak`

- 役割: TODO
- 貢献: TODO
- 課題と解決方法: TODO

### `kofujita`

- 役割: TODO
- 貢献: TODO
- 課題と解決方法: TODO

### `yonuma`

- 役割: TODO
- 貢献: TODO
- 課題と解決方法: TODO

## 既知の制限

- ローカル開発用プロキシでは自己署名証明書を使用します。
- 在席状態はバックエンドのプロセス単位で管理し、データベースには保存しません。
- デフォルトのレート制限とログイン失敗回数はメモリ上で管理し、単一アプリケーションインスタンスを前提としています。
- TODO: 評価前に追加の既知の制限を記載してください。

## ライセンス

TODO: プロジェクトのライセンスとクレジット情報を追加してください。
