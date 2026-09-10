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

test("SockJS/STOMPで接続してメッセージを変換できる", () => {
  const received = [];
  connect_ws(42, (payload) => received.push(payload));
  const first = FakeWebSocket.instances[0];

  assert.match(first.url, /^wss:\/\/example\.com\/ws\/001\/.+\/websocket$/);
  assert.equal(send_ws("before open"), false);

  first.readyState = FakeWebSocket.OPEN;
  assert.equal(send_ws("hello"), true);
  assert.equal(first.sent[0], "hello");

  first.onmessage?.({ data: "o" });
  assert.match(first.sent[1], /^\["CONNECT\\naccept-version:1\.2\\nheart-beat:0,0\\n\\n\\u0000"\]$/);

  first.onmessage?.({ data: 'a["CONNECTED\\nversion:1.2\\n\\n\\u0000"]' });
  assert.match(first.sent[2], /destination:\/user\/queue\/rooms\/42\/messages/);
  assert.match(first.sent[3], /destination:\/user\/queue\/rooms\/42\/presence/);

  const stompMessage = [
    "MESSAGE",
    "subscription:sub-room-messages",
    "message-id:007",
    "",
    "{\"type\":\"chat:message\",\"payload\":{\"roomId\":42,\"sender\":{\"name\":\"Alice\"},\"content\":\"hello\"}}\u0000",
  ].join("\n");
  first.onmessage?.({ data: `a[${JSON.stringify(stompMessage)}]` });
  assert.deepEqual(received, ['{"type":"msg","room_id":42,"user":"Alice","message":"hello"}']);

  const presenceMessage = [
    "MESSAGE",
    "subscription:sub-room-presence",
    "message-id:008",
    "",
    "{\"type\":\"room:user_joined\",\"payload\":{\"roomId\":42,\"userId\":7,\"name\":\"Bob\",\"iconUrl\":null,\"online\":true}}\u0000",
  ].join("\n");
  first.onmessage?.({ data: `a[${JSON.stringify(presenceMessage)}]` });
  assert.deepEqual(received[1], '{"type":"presence","room_id":42,"user_id":7,"user":"Bob","icon_url":"","online":true}');

  close_ws();
  connect_ws(42, () => {});
  assert.equal(FakeWebSocket.instances.length, 2);
});
