# online-workspace

オンラインで他のユーザーと一緒に作業できるWebアプリケーションです。

最終版の要件に基づき、React + Spring Boot + MyBatis + PostgreSQLで開発しています。
旧MVPのThymeleaf実装は削除し、Reactから利用するREST APIを中心に実装します。

仕様は以下のドキュメントを参照してください。

- [要件定義](docs/requirements.md)
- [DBスキーマ](docs/db_schema.md)
- [システム構成](docs/system_architecture.pdf)

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
