_このプロジェクトは、42のカリキュラムの一環として yehara、tmuranak、kofujita、yonuma によって作成されました。_

# Online Workspace (ft_transcendence)

[English version](README.md)

## 概要

Online Workspace は、オンラインで他のユーザーと一緒に作業するためのコラボレーションプラットフォームです。ユーザーは作業ルームを作成・検索・参加でき、参加者の在席状況を確認しながらリアルタイムチャットでコミュニケーションできます。

フレンド関係を前提にせず、ログイン済みユーザーが同じ目的の作業ルームに参加できることを目的としています。安全なREST API、WebSocketによるリアルタイム通信、監視可能なコンテナ環境を重視しています。

### 主な機能

- ユーザー登録、ログイン、ログアウト
- プロフィール編集、アイコン画像アップロード、ユーザー検索、公開設定
- ルームの作成、一覧、参加、退出
- チャット機能
- フレンド管理とフレンドのオンライン状態表示

## 実行手順

```bash
docker compose up --build
```

開発用プロキシには次のURLからアクセスできます。

- HTTPSアプリケーション: `https://localhost:8443`

プロキシでは開発用の自己署名証明書を使用します。起動時にブラウザで証明書の例外設定が必要になる場合があります。

### 監視環境の起動

監視サービスは `compose.observability.yaml` に定義されています。必要な変数を設定して次を実行します。

```bash
MANAGEMENT_API_KEY="change-me" \
GRAFANA_ADMIN_PASSWORD="change-me" \
GRAFANA_ALERT_DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..." \
docker compose -f compose.yaml -f compose.observability.yaml up prometheus grafana
```

## 参考資料

### プロジェクト内ドキュメント

- [要件定義](docs/requirements.md)
- [API契約](docs/openapi.yaml)
- [API開発手順](docs/api_development.md)
- [WebSocketイベント契約](docs/websocket_events.md)
- [DBスキーマ](docs/db_schema.md)
- [Webセキュリティ方針](docs/web_security.md)
- [アーキテクチャ](docs/architecture.md)
- [42 subject](docs/ft_transcendence.pdf)

### 外部参考資料

- [Spring Boot ドキュメント](https://spring.io/projects/spring-boot)
- [Gleam ドキュメント](https://gleam.run/documentation/)
- [PostgreSQL ドキュメント](https://www.postgresql.org/docs/)
- [Prometheus ドキュメント](https://prometheus.io/docs/)
- [Grafana ドキュメント](https://grafana.com/docs/)

### AIの利用

本プロジェクトでは、ChatGPTやGitHub CopilotなどのAIツールを補助的に利用しました。主な用途は次のとおりです。

- Nginxの設定とDockerfileの初期セットアップの補助
- ELKスタックとSpring Bootバックエンドの統合に関するトラブルシューティングのアイデア出し
- バックエンドの単体テスト用ボイラープレートコードの生成

AIが生成した内容は、チームでレビュー、テスト、修正を行い、採用したコードは内容を理解したうえで使用しています。

## チーム情報

チームは以下の4名で構成されています。役割と主な担当は次のとおりです。

| メンバー | 役割 | 担当 |
| --- | --- | --- |
| `yehara` | プロダクトオーナー、開発者 | 要件定義、API、セキュリティ、インフラ、監視 |
| `tmuranak` | テックリード、開発者 | Gleam/Lustreフロントエンド、WebSocketクライアント |
| `kofujita` | プロジェクトマネージャー、開発者 | 開発環境、データベース、バックエンドのレビュー、チーム支援 |
| `yonuma` | 開発者 | 認証、プロフィール、フレンド、ルーム、在席状態のリアルタイム更新 |

## プロジェクト管理

- コミュニケーション手段: Discord
- 定例ミーティング: 週次ミーティング
- タスク管理ツール: GitHub Issues、GitHub Projects
- コードレビューの運用: mainブランチへの変更はPull Request経由で行い、少なくとも1名のピアレビューを受ける
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
| 認証 | 登録、ログイン、ログアウト、セッション、CSRFトークン | `yehara`, `yonuma` |
| プロフィール管理 | 公開設定、自己紹介、作業カテゴリ、アイコン管理 | `yehara`, `yonuma` |
| ユーザー検索 | 名前によるページネーション付き検索と公開プロフィール | `yonuma` |
| ルーム管理 | 作成、一覧、絞り込み、詳細、更新、終了 | `yehara`, `yonuma` |
| ルーム参加 | 参加、退出、参加者一覧、人数制限、アクセス制御 | `yehara`, `yonuma` |
| チャット | ページネーション付き履歴とメッセージ検証 | `yehara`, `tmuranak` |
| リアルタイム更新 | チャット、在席状態、参加人数、ルーム作成、フレンド状態 | `yehara`, `tmuranak`, `yonuma` |
| フレンド管理 | 追加、一覧、解除、オンライン状態 | `yehara`, `yonuma` |
| 公開API | OpenAPIで定義したセッション/API key認証とレート制限付きREST API | `yehara` |
| 可観測性 | ヘルスチェック、Prometheus、Grafana、アラート、ELKログ | `yehara` |

## モジュール

現在のモジュール計画は14ポイントです。担当者は各行に記載しています。
モジュール要件のチェック項目は[こちらのスプレッドシート](https://docs.google.com/spreadsheets/d/1ScHkTosDOwcBnFoCIuz26hpp9bQ9IQu2MThSVHGN9mE/edit?pli=1&gid=0#gid=0)を参照してください。

| モジュール | 区分 | 実装内容 | 担当 |
| --- | ---: | --- | --- |
| フロントエンド・バックエンドフレームワーク | Major, 2点 | Gleam/LustreフロントエンドとSpring Bootバックエンド | `yehara`, `tmuranak` |
| リアルタイム機能 | Major, 2点 | チャットと在席状態のSTOMP over SockJS/WebSocket | `yehara`, `tmuranak`, `yonuma` |
| ユーザー間機能 | Major, 2点 | プロフィール、フレンド、ルーム参加、チャット | `yehara`, `yonuma` |
| 公開API | Major, 2点 | API keyとレート制限を備えたOpenAPI REST API | `yehara` |
| ログ管理基盤 | Major, 2点 | Elasticsearch、Logstash、Kibana | `yehara` |
| 監視システム | Major, 2点 | Prometheus、Grafana、ダッシュボード、アラート | `yehara` |
| ファイルアップロード・管理 | Minor, 1点 | アイコンのアップロード、保存、取得、削除 | `yehara`, `yonuma` |
| ヘルスチェック・ステータス | Minor, 1点 | 保護されたActuatorのhealth / Prometheus endpoint | `yehara` |
| **合計** | **14点** |  |  |

### モジュール選定の考え方

- Webフレームワーク: Gleam/LustreとSpring Bootを採用し、型安全なUIと堅牢なAPIを構築する
- リアルタイム機能: WebSocketでチャット、在席状態、参加人数の変化を接続中のユーザーへ配信する
- ユーザー間機能: プロフィール、フレンド、ルーム、チャットを組み合わせて作業相手と交流できるようにする
- 公開API: OpenAPI、API key、レート制限を備えたREST APIを提供する
- ログ管理・監視: ELK、Prometheus、Grafana、アラートによって運用時の状態を確認できるようにする
- ファイル管理・ヘルスチェック: アイコン画像の管理と保護された運用エンドポイントを提供する

## 個人の貢献

以下にメンバーごとの主な貢献と、対応した課題を記載します。

### tmuranak
  - Gleamを用いたフロントエンドアーキテクチャの設計。
  - リアルタイムWebSocketクライアントロジック（ws.js）の実装。
  - *直面した課題:*
    - サーバーサイドのAPIとの接続の困難さがあると感じたため、設計段階から必要になる可能性のある機能を積極的に切り分けた

### yonuma
  - ユーザー登録、ログイン・ログアウト、セッション認証などのユーザー認証機能を実装した。
  - フレンド検索・追加・解除、プロフィール・マイページ表示、フレンド状態やアイコン表示を実装した。
  - ルーム一覧・詳細・入退室、F5やタブ閉じへの対応、WebSocketによる参加人数のリアルタイム更新を実装した。
  - API通知とWebSocket通知による参加人数の二重カウント、非公開プロフィールのフレンド状態、外部アイコン表示に関する問題を解決した。
  - Spring Securityを触ったことがなかったので最初は何をしているのか分からなかった。この分野を実務で触っているチームメンバーのyeharaに相談し、疑問点を解消するように努めた。

### kofujita
  - 環境周りのレビュー。
  - データベース・バックエンドのレビュー。
  - 人手が足りていない場所のお手伝い。
  - 細かい場所はコミット参照。
  - *直面した課題:*
    - 限られた時間の中で自分の作業と他のメンバーのレビューを両立する必要があったため、時間を確保してレビューを進めた。

### yehara
  - *直面した課題:*
    - 当初予定していた機能に対し、残された時間が少なかったため、要件満たせるようにしつつ不要な機能を削除した

## 既知の制限

- ローカル開発用プロキシでは自己署名証明書を使用します。
- 在席状態はバックエンドのプロセス単位で管理し、データベースには保存しません。
- デフォルトのレート制限とログイン失敗回数はメモリ上で管理し、単一アプリケーションインスタンスを前提としています。
