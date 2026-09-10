let socket = null;
let messageDispatch = null;

function sockFrame(stompFrame) {
  return JSON.stringify([stompFrame]);
}

function stompFrame(lines, body = "") {
  return `${lines.join("\n")}\n\n${body}\0`;
}

function subscribeChat(connection, roomId) {
  connection.send(
    sockFrame(
      stompFrame([
        "SUBSCRIBE",
        "id:sub-room-messages",
        `destination:/user/queue/rooms/${roomId}/messages`,
        "ack:auto",
      ])
    )
  );
}

function subscribePresence(connection, roomId) {
  connection.send(
    sockFrame(
      stompFrame([
        "SUBSCRIBE",
        `id:sub-room-presence-${roomId}`,
        `destination:/topic/rooms/${roomId}/presence`,
        "ack:auto",
      ])
    )
  );
}

function parseSockJsMessages(raw) {
  if (raw === "o" || raw === "h") return [];
  if (!raw.startsWith("a")) return [];
  try {
    return JSON.parse(raw.slice(1));
  } catch (_) {
    return [];
  }
}

function parseStompChatMessage(frame) {
  const separator = frame.indexOf("\n\n");
  if (separator < 0) return null;

  const bodyWithNull = frame.slice(separator + 2);
  const body = bodyWithNull.endsWith("\0")
    ? bodyWithNull.slice(0, -1)
    : bodyWithNull;

  try {
    const event = JSON.parse(body);
    if (event.type !== "chat:message" || !event.payload) return null;
    return JSON.stringify({
      type: "msg",
      room_id: event.payload.roomId,
      user: event.payload.sender?.name || "unknown",
      message: event.payload.content || "",
    });
  } catch (_) {
    return null;
  }
}

function parseStompPresenceMessage(frame) {
  const separator = frame.indexOf("\n\n");
  if (separator < 0) return null;

  const bodyWithNull = frame.slice(separator + 2);
  const body = bodyWithNull.endsWith("\0")
    ? bodyWithNull.slice(0, -1)
    : bodyWithNull;

  try {
    const event = JSON.parse(body);
    if (
      !event.type ||
      !event.type.startsWith("room:user_") ||
      !event.payload
    ) {
      return null;
    }
    return JSON.stringify({
      type: "presence",
      room_id: event.payload.roomId,
      user_id: event.payload.userId,
      user: event.payload.name,
      icon_url: event.payload.iconUrl || "",
      online: event.payload.online,
    });
  } catch (_) {
    return null;
  }
}

function parseStompRoomCreatedMessage(frame) {
  const separator = frame.indexOf("\n\n");
  if (separator < 0) return null;

  const bodyWithNull = frame.slice(separator + 2);
  const body = bodyWithNull.endsWith("\0")
    ? bodyWithNull.slice(0, -1)
    : bodyWithNull;

  try {
    const event = JSON.parse(body);
    if (event.type !== "room:created" || !event.payload) return null;
    return JSON.stringify({ type: "room:created", payload: event.payload });
  } catch (_) {
    return null;
  }
}

function open_ws(roomIds, chatRoomId, dispatch) {
  messageDispatch = dispatch;
  if (
    socket &&
    (socket.readyState === WebSocket.CONNECTING ||
      socket.readyState === WebSocket.OPEN)
  ) {
    if (socket.readyState === WebSocket.OPEN && chatRoomId !== null) {
      subscribeChat(socket, chatRoomId);
      subscribePresence(socket, chatRoomId);
    } else if (socket.readyState === WebSocket.OPEN) {
      roomIds.forEach((roomId) => subscribePresence(socket, roomId));
    }
    return;
  }

  const url = new URL(
    `/ws/001/${Date.now().toString(36)}${Math.random()
      .toString(36)
      .slice(2)}/websocket`,
    window.location.href
  );
  url.protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const connection = new WebSocket(url);
  socket = connection;

  connection.onmessage = (event) => {
    if (event.data === "o") {
      connection.send(
        sockFrame(stompFrame(["CONNECT", "accept-version:1.2", "heart-beat:0,0"]))
      );
      return;
    }

    const frames = parseSockJsMessages(event.data);
    for (const frame of frames) {
      if (frame.startsWith("CONNECTED")) {
        if (chatRoomId !== null) {
          subscribeChat(connection, chatRoomId);
        }
        if (chatRoomId === null) {
          connection.send(
            sockFrame(
              stompFrame([
                "SUBSCRIBE",
                "id:sub-room-created",
                "destination:/topic/rooms",
                "ack:auto",
              ])
            )
          );
        }
        roomIds.forEach((roomId) => {
          subscribePresence(connection, roomId);
        });
        continue;
      }

      if (frame.startsWith("MESSAGE")) {
        const parsed = parseStompChatMessage(frame);
        const presence = parsed === null ? parseStompPresenceMessage(frame) : null;
        const roomCreated =
          parsed === null && presence === null
            ? parseStompRoomCreatedMessage(frame)
            : null;
        if (parsed !== null && messageDispatch !== null) messageDispatch(parsed);
        if (presence !== null && messageDispatch !== null) messageDispatch(presence);
        if (roomCreated !== null && messageDispatch !== null) messageDispatch(roomCreated);
      }
    }
  };

  connection.onclose = () => {
    if (socket === connection) socket = null;
  };
}

export function connect_ws(roomId, dispatch) {
  open_ws([roomId], roomId, dispatch);
}

export function connect_room_list(roomIdsJson, dispatch) {
  open_ws(JSON.parse(roomIdsJson), null, dispatch);
}

export function send_ws(message) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(message);
    return true;
  }
  return false;
}

export function close_ws() {
  if (socket) {
    const connection = socket;
    socket = null;
    connection.close();
  }
}
