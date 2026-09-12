# online-workspace

オンラインで他のユーザーと一緒に作業できるWebアプリケーションです。

最終版の要件に基づき、Gleam + Spring Boot + MyBatis + PostgreSQLで開発しています。
旧MVPのThymeleaf実装は削除し、画面を返すMVC Controllerは置かず、Gleamから利用するREST APIを実装します。

仕様は以下のドキュメントを参照してください。

- [要件定義](docs/requirements.md)
- [DBスキーマ](docs/db_schema.md)
- [退会時のデータ保持方針](docs/account_withdrawal_data_policy.md)
- [システム構成](docs/system_architecture.pdf)
- [ブランチ命名規則](docs/branch_naming_convention.md)

## 技術スタック

- Frontend: gleam / lustre / Tailwind CSS
- Backend: Spring Boot / Spring Security / MyBatis / Bean Validation / WebSocket
- Database: PostgreSQL / Flyway

## バックエンド構成

Controllerは機能単位のパッケージに配置します。

```text
controllers/
├── auth/
├── members/
├── messages/
├── rooms/
├── users/
└── workhistory/
```

## ローカル起動

### Docker Compose で起動

初回は `.env.example` をコピーし、パスワードとAPIキーを変更してください。
`.env` はGit管理対象外です。
proxy イメージのビルド時にフロントエンドを本番用にビルドし、生成された静的ファイルを nginx から配信します。

```bash
cp .env.example .env
# .envを編集して、change-this-* の値を変更する
docker compose up --build
```

自己署名証明書は `proxy` コンテナの起動時に自動生成されます。

起動後のURL:

- Proxy: https://localhost:${PROXY_HTTPS_PORT:-8443}（自己署名証明書）
- HTTP redirect: http://localhost:${PROXY_HTTP_PORT:-8088}
- Backend: http://localhost:${BACKEND_HOST_PORT:-8080}
- PostgreSQL: localhost:${POSTGRES_HOST_PORT:-5432}

コンテナ構成:

- `proxy`: 静的ファイル配信と nginx reverse proxy
- `backend`: Spring Boot dev server
- `db`: PostgreSQL 16
- `ws-mock`: チャット機能テスト用 WebSocket モックサーバー

HTTPS / WSS、Cookie、CSRF、CORS、セキュリティヘッダー、監査ログの方針と
確認方法は[Webセキュリティ方針](docs/web_security.md)を参照してください。

### Backend 単体で起動

1. PostgreSQL を起動し、接続情報を環境変数で設定します。Composeでは `.env` の値が使われます。
   - `DB_URL` (default: `jdbc:postgresql://localhost:5432/postgres`)
   - `DB_USERNAME` (default: `postgres`)
   - `DB_PASSWORD` (`.env` の `POSTGRES_PASSWORD`)
   - `MAIL_HOST` (default: `localhost`)
   - `MAIL_PORT` (default: `1025`)
2. アプリを起動します。

ブラウザから `http://localhost:8080` のバックエンドへ直接接続して認証APIを確認する場合は、
`SESSION_COOKIE_SECURE=false` を設定してください。HTTPSのComposeプロキシ経由ではtrue、
本番でも `SESSION_COOKIE_SECURE=true` を設定します。

```bash
SESSION_COOKIE_SECURE=false ./gradlew bootRun
```

初回起動時に Flyway がマイグレーションを実行します。

## ヘルスチェックとメトリクス

`MANAGEMENT_API_KEY` を設定すると、次の監視エンドポイントを
`X-API-Key` ヘッダー付きで利用できます。未設定時はアクセスを拒否します。

- `GET /actuator/health`: アプリケーションとDBの稼働状態
- `GET /actuator/prometheus`: Prometheus text exposition format

```bash
curl -H "X-API-Key: $MANAGEMENT_API_KEY" http://localhost:8080/actuator/health
curl -H "X-API-Key: $MANAGEMENT_API_KEY" http://localhost:8080/actuator/prometheus
```

`/actuator/prometheus` では `http.server.requests`（応答時間・status別件数）、JVM/process の
CPU・メモリ、`hikaricp.connections`（DB接続）、`tomcat.connections.current`
（同時接続）、`websocket.connections.active`（WebSocket接続）を確認できます。
Prometheus形式では各メトリクス名のドットがアンダースコアへ変換されます。

### Prometheus / Grafana

監視構成は `.env` の `MANAGEMENT_API_KEY` と `GRAFANA_ADMIN_PASSWORD` を使用します。

```bash
set -a && source .env && set +a
docker compose -f compose.yaml -f compose.observability.yaml up prometheus grafana
```

起動後のURL:

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000（ユーザー名は `admin`。`GRAFANA_ADMIN_USER` で変更可能）

Grafana の匿名アクセスは無効です。Prometheus データソース、
`Online Workspace Monitoring` ダッシュボード、および次のアラートは起動時に自動設定されます。

- 5xx率が5分間5%を超えた
- Backendのメトリクスを2分間取得できない

通知先のDiscord Webhook URLは `GRAFANA_ALERT_DISCORD_WEBHOOK_URL` で設定します。
メトリクスの保持期間は15日です。

## フロントエンド開発方針

nginx がビルド済みの Gleam 静的ファイルを配信します。Gleam 用の常駐アプリケーションサーバーは立てません。

```text
Browser -> nginx
  ├─ /        -> dist/index.html, dist/assets/*
  ├─ /api/*   -> Spring Boot
  └─ /ws      -> Spring Boot
```

フロントエンドのビルドは `proxy` イメージの build stage で実行します。Node.js / Gleam の実行環境はコンテナ内に用意し、ホストへビルドツールを要求しません。

```text
Browser -> proxy container
  ├─ /        -> /var/www/static
  ├─ /api/*   -> backend container
  └─ /ws      -> backend container
```

Lustre のビルドは Node.js / Gleam 環境で実行し、生成された `dist/` を nginx の静的配信対象にします。

```text
gleam source -> npm run build -> proxy image -> nginx
```

## テスト

```bash
./gradlew test
```

## API 契約

REST API の契約は `docs/openapi.yaml` で管理する。API を変更する Issue では実装と同じ PR で仕様を更新し、次のコマンドで検証する。

```bash
npx --yes @redocly/cli@2.43.2 lint online-workspace@v1
npx --yes openapi-typescript@7.13.0
```

詳細は [API 契約の更新手順](docs/api_development.md) を参照する。

### Swagger UI

Backend 起動後、次のURLで `docs/openapi.yaml` を表示できる。

- Swagger UI: https://localhost:8443/swagger-ui.html
- OpenAPI YAML: https://localhost:8443/openapi.yaml

ブラウザ向け API を `Try it out` で確認する場合は、先に同じブラウザで https://localhost:8443/login からログインする。session cookie は同一 origin のリクエストに自動で付与される。`/api/v1/public/rooms` 以下の公開 API はセッション認証では利用できないため、Swagger UI 右上の `Authorize` から `X-API-Key` を設定する。

セッション認証で状態を変更する API では CSRF token が必要になる。Swagger UI は `XSRF-TOKEN` cookie の値を `X-CSRF-TOKEN` header として送信する設定になっている。API key 必須の公開 API では CSRF token は不要。
