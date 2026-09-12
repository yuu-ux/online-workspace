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

const ws = await import("./ws.js");

const {
  close_ws,
  connect_friend_presence,
  connect_room_list,
  connect_ws,
  send_ws,
} = ws;

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
  assert.match(first.sent[3], /destination:\/topic\/rooms\/42\/presence/);

  const stompMessage = [
    "MESSAGE",
    "subscription:sub-room-messages",
    "message-id:007",
    "",
    "{\"type\":\"chat:message\",\"payload\":{\"roomId\":42,\"sender\":{\"name\":\"Alice\"},\"content\":\"hello\",\"sentAt\":\"2026-09-10T05:32:00Z\"}}\u0000",
  ].join("\n");
  first.onmessage?.({ data: `a[${JSON.stringify(stompMessage)}]` });
  assert.deepEqual(received, ['{"type":"msg","room_id":42,"user":"Alice","message":"hello","sent_at":"2026-09-10T05:32:00Z","icon_url":""}']);

  const presenceMessage = [
    "MESSAGE",
    "subscription:sub-room-presence",
    "message-id:008",
    "",
    "{\"type\":\"room:user_joined\",\"payload\":{\"roomId\":42,\"userId\":7,\"name\":\"Bob\",\"iconUrl\":null,\"online\":true}}\u0000",
  ].join("\n");
  first.onmessage?.({ data: `a[${JSON.stringify(presenceMessage)}]` });
  assert.deepEqual(received[1], '{"type":"presence","room_id":42,"user_id":7,"user":"Bob","icon_url":"","online":true}');

  const memberCountMessage = [
    "MESSAGE",
    "subscription:sub-room-presence-42",
    "message-id:009",
    "",
    "{\"type\":\"room:member_count_changed\",\"payload\":{\"roomId\":42,\"currentMembers\":2}}\u0000",
  ].join("\n");
  first.onmessage?.({ data: `a[${JSON.stringify(memberCountMessage)}]` });
  assert.deepEqual(received[2], '{"type":"room:member_count_changed","room_id":42,"current_members":2}');

  close_ws();
  connect_ws(42, () => {});
  assert.equal(FakeWebSocket.instances.length, 2);
  close_ws();
});

test("接続中にルームへ移動した場合も移動先の購読を登録する", () => {
  connect_room_list("[10,11]", () => {});
  const connection = FakeWebSocket.instances[FakeWebSocket.instances.length - 1];

  connect_ws(42, () => {});
  connection.readyState = FakeWebSocket.OPEN;
  connection.onmessage?.({ data: "o" });
  connection.onmessage?.({ data: 'a["CONNECTED\\nversion:1.2\\n\\n\\u0000"]' });

  assert.ok(connection.sent.some((message) => message.includes("/user/queue/rooms/42/messages")));
  assert.ok(connection.sent.some((message) => message.includes("/topic/rooms/42/presence")));
  assert.equal(connection.sent.some((message) => message.includes("/topic/rooms/10/presence")), false);
  assert.equal(connection.sent.some((message) => message.includes("destination:/topic/rooms\\n")), false);

  close_ws();
});

test("フレンドのオンライン状態を購読して変換できる", () => {
  const received = [];
  connect_friend_presence((payload) => received.push(payload));
  const connection = FakeWebSocket.instances[FakeWebSocket.instances.length - 1];

  connection.readyState = FakeWebSocket.OPEN;
  connection.onmessage?.({ data: "o" });
  connection.onmessage?.({ data: 'a["CONNECTED\\nversion:1.2\\n\\n\\u0000"]' });

  assert.ok(connection.sent.some((message) => message.includes("/user/queue/friends/presence")));

  const presenceMessage = [
    "MESSAGE",
    "subscription:sub-friend-presence",
    "message-id:010",
    "",
    '{"type":"friend:presence_changed","payload":{"userId":7,"online":true}}\u0000',
  ].join("\n");
  connection.onmessage?.({ data: `a[${JSON.stringify(presenceMessage)}]` });

  assert.deepEqual(received, ['{"type":"friend_presence","user_id":7,"online":true}']);
  close_ws();
});

test("接続済みソケットの画面遷移で購読を入れ替えられる", () => {
  connect_friend_presence(() => {});
  const connection = FakeWebSocket.instances[FakeWebSocket.instances.length - 1];
  connection.readyState = FakeWebSocket.OPEN;
  connection.onmessage?.({ data: "o" });
  connection.onmessage?.({ data: 'a["CONNECTED\\nversion:1.2\\n\\n\\u0000"]' });

  connect_room_list("[10]", () => {});

  assert.ok(connection.sent.some((message) => message.includes("UNSUBSCRIBE\\nid:sub-friend-presence")));
  assert.ok(connection.sent.some((message) => message.includes("/topic/rooms/10/presence")));
  assert.equal(connection.sent.some((message) => message.includes("/user/queue/friends/presence")), true);

  close_ws();
});

test("操作本人向けの通知を購読して受信できる", () => {
  assert.equal(typeof ws.connect_notifications, "function");

  const received = [];
  ws.connect_notifications((payload) => received.push(payload));
  const connection = FakeWebSocket.instances[FakeWebSocket.instances.length - 1];
  connection.readyState = FakeWebSocket.OPEN;
  connection.onmessage?.({ data: "o" });
  connection.onmessage?.({ data: 'a["CONNECTED\\nversion:1.2\\n\\n\\u0000"]' });

  assert.ok(connection.sent.some((message) => message.includes("/user/queue/notifications")));

  const notificationMessage = [
    "MESSAGE",
    "subscription:sub-notifications",
    "message-id:011",
    "",
    '{"type":"notification","message":"ルームを作成しました。"}\u0000',
  ].join("\\n");
  connection.onmessage?.({ data: `a[${JSON.stringify(notificationMessage)}]` });

  assert.deepEqual(received, ['{"type":"notification","message":"ルームを作成しました。"}']);
  close_ws();
});
