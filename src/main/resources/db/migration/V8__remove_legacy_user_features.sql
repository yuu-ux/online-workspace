-- ブロック・通報・招待機能の残存データベースオブジェクトを整理する。
-- 既存環境で過去の削除マイグレーションが未適用でも実行できるようにする。

DROP INDEX IF EXISTS idx_blocks_blocked_blocker;

DROP TABLE IF EXISTS room_invites;
DROP TABLE IF EXISTS admin_actions;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS blocks;
DROP TABLE IF EXISTS report_statuses;
DROP TABLE IF EXISTS report_reasons;
DROP TABLE IF EXISTS admin_action_types;
