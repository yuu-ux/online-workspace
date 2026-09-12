# WebSocket Event Contract

## Connection

- Endpoint: `/ws`
- Protocol: STOMP over WebSocket / SockJS
- Authentication: Spring Security `JSESSIONID` session cookie
- CSRF: `/ws/**` is excluded from CSRF validation

The frontend connects using SockJS WebSocket transport and subscribes to the required destinations after the STOMP `CONNECT`. Chat messages are sent through `POST /api/v1/rooms/{roomId}/messages`.

## chat:message

Room participants subscribe to `/user/queue/rooms/{roomId}/messages`. After a message is created, the following event is delivered to the participants at the time of sending.

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
    "content": "Hello",
    "sentAt": "2026-08-30T00:00:00Z"
  }
}
```

The `payload` uses the same format as the `ChatMessage` schema in `docs/openapi.yaml`.

## room:user_joined / room:user_left

The server delivers the same event to the following two destinations.

- `/topic/rooms/{roomId}/presence`
- `/user/queue/rooms/{roomId}/presence`

The payload uses the following format.

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

`room:user_left` uses the same payload, and `online` represents the state after the event occurs.

## room:member_count_changed

The current number of participants is delivered to `/topic/rooms/{roomId}/presence` in the following format.

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

When a room is created, the following event is delivered to `/topic/rooms`. The `payload` uses the same format as the `RoomDetail` schema in `docs/openapi.yaml`.

## friend:presence_changed

When a friend's online status changes, the event is delivered to the target user's `/user/queue/friends/presence`.

```json
{
  "type": "friend:presence_changed",
  "payload": {
    "userId": 7,
    "online": true
  }
}
```

## Online Status

- A user with one or more authenticated STOMP connections is considered online.
- Connections are counted individually across multiple tabs, and the user is marked offline only when the last connection is disconnected.
- A reconnection is counted as a new connection. Forced browser termination and communication timeouts are reflected through the disconnect events issued by Spring.
- The status is not stored in the database. After an application restart, everyone starts offline, so an incorrect online status is not persisted.
- The status is managed within a single application instance. When scaling to multiple instances, move it to a shared store.
