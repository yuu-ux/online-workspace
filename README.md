# online-workspace

オンラインで他のユーザーと一緒に作業できるWebアプリケーションです。

最終版の要件に基づき、React + Spring Boot + MyBatis + PostgreSQLで開発しています。
旧MVPのThymeleaf実装は削除し、Reactから利用するREST APIを中心に実装します。

仕様は以下のドキュメントを参照してください。

- [要件定義](docs/requirements.md)
- [DBスキーマ](docs/db_schema.md)
- [退会時のデータ保持方針](docs/account_withdrawal_data_policy.md)
- [システム構成](docs/system_architecture.pdf)
- [ブランチ命名規則](docs/branch_naming_convention.md)

## 技術スタック

- Frontend: React / React Router / Tailwind CSS
- Backend: Spring Boot / Spring Security / MyBatis / Bean Validation / WebSocket
- Database: PostgreSQL / Flyway

## ローカル起動

### Docker Compose で起動

開発時は proxy / backend / frontend / db / maildev をまとめて起動できます。

```bash
docker compose up
```

ホストの UID/GID が `1000:1000` 以外の Linux 環境では、bind mount
への書き込み権限を合わせて起動します。

```bash
HOST_UID="$(id -u)" HOST_GID="$(id -g)" docker compose up
```

起動後のURL:

- Proxy: http://localhost:8088
- Backend: http://localhost:8080
- MailDev: http://localhost:1080
- PostgreSQL: localhost:5432

コンテナ構成:

- `proxy`: 開発用 nginx reverse proxy
- `frontend`: Vite React dev server
- `backend`: Spring Boot dev server
- `db`: PostgreSQL 16
- `maildev`: 開発用メール確認サーバー

### Backend 単体で起動

1. PostgreSQL を起動し、接続情報を環境変数で設定します（未設定時はデフォルト値を利用）。
   - `DB_URL` (default: `jdbc:postgresql://localhost:5432/postgres`)
   - `DB_USERNAME` (default: `postgres`)
   - `DB_PASSWORD` (default: `password`)
   - `MAIL_HOST` (default: `localhost`)
   - `MAIL_PORT` (default: `1025`)
2. アプリを起動します。

```bash
./gradlew bootRun
```

初回起動時に Flyway がマイグレーションを実行します。

## ヘルスチェックとメトリクス

`MANAGEMENT_API_KEY` を設定すると、次の監視エンドポイントを
`X-API-Key` ヘッダー付きで利用できます。未設定時はアクセスを拒否します。

- `GET /health`: アプリケーションとDBの稼働状態
- `GET /metrics`: Prometheus text exposition format

```bash
curl -H "X-API-Key: $MANAGEMENT_API_KEY" http://localhost:8080/health
curl -H "X-API-Key: $MANAGEMENT_API_KEY" http://localhost:8080/metrics
```

`/metrics` では `http.server.requests`（応答時間・status別件数）、JVM/process の
CPU・メモリ、`hikaricp.connections`（DB接続）、`tomcat.connections.current`
（同時接続）、`websocket.connections.active`（WebSocket接続）を確認できます。
Prometheus形式では各メトリクス名のドットがアンダースコアへ変換されます。

## フロントエンド開発方針

本番環境では nginx がビルド済みの React 静的ファイルを配信します。React 用の常駐アプリケーションサーバーは立てません。

```text
Browser -> nginx
  ├─ /        -> dist/index.html, dist/assets/*
  ├─ /api/*   -> Spring Boot
  └─ /ws      -> Spring Boot
```

開発時は `proxy` コンテナ経由で `frontend` コンテナの Vite dev server にアクセスします。作業者ごとの Node.js バージョン差を避けるため、Node.js 環境はコンテナ内に用意します。

```text
Browser -> proxy container
  ├─ /        -> frontend container
  │              ├─ React 開発用ファイル配信
  │              └─ HMR
  ├─ /api/*   -> backend container
  └─ /ws      -> backend container
```

React のビルドは Node.js 環境で実行し、生成された `dist/` を nginx の静的配信対象にします。

```text
React source -> npm run build -> dist/ -> nginx
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

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI YAML: http://localhost:8080/openapi.yaml

認証が必要な API を `Try it out` で確認する場合は、先に同じブラウザで http://localhost:8080/login からログインする。session cookie は同一 origin のリクエストに自動で付与される。API key 認証は Swagger UI 右上の `Authorize` から `X-API-Key` を設定する。

状態を変更する API では CSRF token が必要になる。Swagger UI は `XSRF-TOKEN` cookie の値を `X-CSRF-TOKEN` header として送信する設定になっている。
