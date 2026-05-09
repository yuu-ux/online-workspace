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

1. PostgreSQL を起動し、接続情報を環境変数で設定します（未設定時はデフォルト値を利用）。
   - `DB_URL` (default: `jdbc:postgresql://localhost:5432/postgres`)
   - `DB_USERNAME` (default: `postgres`)
   - `DB_PASSWORD` (default: `password`)
2. アプリを起動します。

```bash
./gradlew bootRun
```

初回起動時に Flyway がマイグレーションを実行します。

## テスト

```bash
./gradlew test
```
