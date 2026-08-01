# online-workspace

Spring Boot + Thymeleaf + MyBatis + PostgreSQL で作成したオンライン作業スペースアプリのMVP実装です。

## 主な機能（MVP）

- ユーザー登録 / ログイン
- ホーム画面での公開ルーム一覧表示
- ルーム作成 / 参加 / 退出
- 作業ルーム内チャット（送信者名・送信時刻表示）
- WebSocket によるリアルタイム更新（入退室・チャット・参加人数更新）
- 画面内通知（成功 / 失敗）
- プライバシーポリシー / 利用規約ページ

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
- Backend MVC: http://localhost:8080
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

## API 契約

REST API の契約は `docs/openapi.yaml` で管理する。API を変更する Issue では実装と同じ PR で仕様を更新し、次のコマンドで検証する。

```bash
npx --yes @redocly/cli@2.43.2 lint online-workspace@v1
npx --yes openapi-typescript@7.13.0
```

詳細は [API 契約の更新手順](docs/api_development.md) を参照する。
