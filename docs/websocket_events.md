# WebSocketイベント契約

## 接続

- Endpoint: `/ws`
- Protocol: STOMP over WebSocket / SockJS
- 認証: Spring Securityのsession cookie

チャット送信は `POST /api/v1/rooms/{roomId}/messages` を使用する。WebSocketは保存済みメッセージの配信に使用する。

## chat:message

ルーム参加者は `/user/queue/rooms/{roomId}/messages` を購読する。メッセージ作成後、送信時点の参加者へ次のイベントを配信する。

```json
{
  "type": "chat:message",
  "payload": {
    "id": 101,
    "roomId": 42,
    "sender": {
      "id": 7,
      "name": "Alice",
      "iconUrl": "https://example.com/alice.png"
    },
    "content": "こんにちは",
    "sentAt": "2026-08-30T00:00:00Z"
  }
}
```

`payload` は `docs/openapi.yaml` の `ChatMessage` schemaと同じ形式とする。
