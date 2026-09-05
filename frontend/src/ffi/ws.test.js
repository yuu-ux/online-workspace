import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = { location: new URL("https://example.com/app") };

class FakeWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;
  static instances = [];

  constructor(url) {
    this.url = url.toString();
    this.readyState = FakeWebSocket.CONNECTING;
    this.sent = [];
    FakeWebSocket.instances.push(this);
  }

  send(message) {
    this.sent.push(message);
  }

  close() {
    this.readyState = FakeWebSocket.CLOSED;
    this.onclose?.();
  }
}

globalThis.WebSocket = FakeWebSocket;

const { close_ws, connect_ws, send_ws } = await import("./ws.js");

test("WebSocket URL、送信結果、切断後の再接続", () => {
  connect_ws("/ws-test", () => {});
  const first = FakeWebSocket.instances[0];

  assert.equal(first.url, "wss://example.com/ws-test");
  assert.equal(send_ws("before open"), false);

  first.readyState = FakeWebSocket.OPEN;
  assert.equal(send_ws("hello"), true);
  assert.deepEqual(first.sent, ["hello"]);

  close_ws();
  connect_ws("/ws-test", () => {});
  assert.equal(FakeWebSocket.instances.length, 2);
});
