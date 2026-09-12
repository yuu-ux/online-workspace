import assert from "node:assert/strict";
import test from "node:test";

import { format_created_at_jst, format_message_time_jst } from "./date.js";

test("送信時刻を日本時間の時分で表示し、不正な日時は表示しない", () => {
  assert.equal(format_message_time_jst("2026-09-10T05:32:00Z"), "14:32");
  assert.equal(format_message_time_jst("2026-09-10T15:02:00Z"), "00:02");
  assert.equal(format_message_time_jst(""), "");
});

test("UTCの作成日時を日本時間の年月日と分へ変換する", () => {
  assert.equal(
    format_created_at_jst("2026-09-10T00:00:00Z"),
    "2026-09-10 09:00"
  );
});

test("日本時間への変換で日付が繰り上がる", () => {
  assert.equal(
    format_created_at_jst("2026-09-10T16:30:00Z"),
    "2026-09-11 01:30"
  );
});

test("不正な日時は元の値を返す", () => {
  assert.equal(format_created_at_jst("invalid"), "invalid");
});
