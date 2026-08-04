# 退会時のデータ保持・削除方針

## 退会直後

- `users.deleted_at` に退会日時を記録する。認証時は必ず `deleted_at IS NULL` のユーザーだけを検索するため、退会済みアカウントはログインできない。
- 現在の HTTP session を破棄し、SecurityContext をクリアする。
- 参加中のルームから退出し、進行中の作業セッションを退会日時で終了する。
- 未使用の招待トークンを無効化する。
- 誤操作防止のため、画面で影響の確認とパスワードの再入力を必須とする。

## 退会から30日後

毎日 03:00 UTC に定期処理を実行し、`deleted_at <= 実行時刻 - 30日` を満たすアカウントを処理する。日数と実行時刻は `ACCOUNT_WITHDRAWAL_RETENTION_DAYS` と `ACCOUNT_WITHDRAWAL_PURGE_CRON` で変更できる。

| 分類 | 対象 | 処理 |
|:--|:--|:--|
| 削除 | `work_sessions` | 作業履歴を物理削除する |
| 削除 | `profiles` | アイコン、自己紹介、公開設定を物理削除する |
| 削除 | `room_members`, `room_invites` | 参加履歴と招待トークンを物理削除する |
| 削除 | `friends`, `blocks` | ソーシャル関係を物理削除する |
| 匿名化 | `users.name`, `users.email`, `users.password_hash` | 名前を退会済み表示に置換し、メールを再利用不可な内部値、パスワードを認証不可な値に置換する |
| 保持 | `users.id`, `users.deleted_at` | 他テーブルの参照整合性と退会事実の確認のため保持する |
| 保持 | `messages` | 送信者を匿名化した上で、別途定める3か月のチャット保持期限まで保持する |
| 保持 | `reports`, `admin_actions` | 通報・対応の監査性と不正利用防止のため、ユーザーID参照を保持する |

`personal_data_purged_at` に完了日時を記録するため、途中失敗時はトランザクションがロールバックされる。次回実行で同じ対象を安全に再処理できる。
