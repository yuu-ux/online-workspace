# WebSocketイベント契約

## 接続

- Endpoint: `/ws`
- Protocol: STOMP over WebSocket / SockJS
- 認証: Spring Securityの `JSESSIONID` session cookie
- CSRF: `/ws/**` はCSRF検証の対象外

フロントエンドはSockJSのWebSocket transportを使用して接続し、STOMP `CONNECT` 後に必要なdestinationを購読する。チャット送信は
`POST /api/v1/rooms/{roomId}/messages` を使用する。

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

サーバーは次の2つのdestinationへ同じイベントを配信する。

- `/topic/rooms/{roomId}/presence`
- `/user/queue/rooms/{roomId}/presence`

payloadは次の形式とする。

```json
{
  "type": "room:user_joined",
  "payload": {
    "roomId": 42,
    "userId": 7,
    "name": "Alice",
    "iconUrl": "https://example.com/alice.png",
    "online": true,
    "occurredAt": "2026-08-30T00:00:00Z"
  }
}
```

`room:user_left` も同じpayloadを使用し、`online` はイベント発生後の状態を表す。

## room:member_count_changed

`/topic/rooms/{roomId}/presence` へ、現在の参加人数を次の形式で配信する。

```json
{
  "type": "room:member_count_changed",
  "payload": {
    "roomId": 42,
    "currentMembers": 3
  }
}
```

## room:created

ルーム作成時に `/topic/rooms` へ次のイベントを配信する。`payload` は
`docs/openapi.yaml` の `RoomDetail` schemaと同じ形式とする。

## friend:presence_changed

フレンドのオンライン状態が変わった場合、対象ユーザーの `/user/queue/friends/presence` へ配信する。

```json
{
  "type": "friend:presence_changed",
  "payload": {
    "userId": 7,
    "online": true
  }
}
```

## オンライン状態

- 認証済みSTOMP接続が1つ以上あるユーザーをオンラインとする。
- 複数タブでは接続を個別に数え、最後の接続が切れた時だけオフラインにする。
- 再接続は新しい接続として数える。ブラウザ強制終了と通信タイムアウトはSpringが発行する切断イベントで反映する。
- 状態はDBへ保存しない。アプリ再起動時は全員オフラインから始まり、誤ったオンライン状態を永続化しない。
- 状態は単一アプリインスタンス内で管理する。複数インスタンス構成へ拡張する場合は共有ストアへ移す。
