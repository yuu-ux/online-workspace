import assert from "node:assert/strict";
import test from "node:test";

const values = new Map();
globalThis.sessionStorage = {
  getItem(key) {
    return values.has(key) ? values.get(key) : null;
  },
  setItem(key, value) {
    values.set(key, value);
  },
  removeItem(key) {
    values.delete(key);
  },
};

const {
  clear_current_room_id,
  get_current_room_id,
  set_current_room_id,
} = await import("./storage.js");

test("現在のルームIDをタブ内に保存して取得できる", () => {
  set_current_room_id("42");
  assert.equal(get_current_room_id(), "42");
});

test("現在のルームIDを削除できる", () => {
  clear_current_room_id();
  assert.equal(get_current_room_id(), "");
});
