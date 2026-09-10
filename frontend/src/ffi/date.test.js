import assert from "node:assert/strict";
import test from "node:test";

import { format_created_at_jst } from "./date.js";

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
