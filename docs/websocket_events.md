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

## room:user_joined / room:user_left

ルーム参加者は `/user/queue/rooms/{roomId}/presence` を購読する。payloadは次の形式とする。

```json
{
  "type": "room:user_joined",
  "payload": {
    "roomId": 42,
    "userId": 7,
    "online": true,
    "occurredAt": "2026-08-30T00:00:00Z"
  }
}
```

`room:user_left` も同じpayloadを使用し、`online` はイベント発生後の状態を表す。

## オンライン状態

- 認証済みSTOMP接続が1つ以上あるユーザーをオンラインとする。
- 複数タブでは接続を個別に数え、最後の接続が切れた時だけオフラインにする。
- 再接続は新しい接続として数える。ブラウザ強制終了と通信タイムアウトはSpringが発行する切断イベントで反映する。
- 状態はDBへ保存しない。アプリ再起動時は全員オフラインから始まり、誤ったオンライン状態を永続化しない。
- 状態は単一アプリインスタンス内で管理する。複数インスタンス構成へ拡張する場合は共有ストアへ移す。
- 状態はフレンド一覧または同じルームの参加者一覧からだけ返す。ルーム参加者一覧APIは参加者本人にだけ許可する。
