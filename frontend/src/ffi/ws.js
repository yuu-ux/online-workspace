let socket = null;

function sockFrame(stompFrame) {
  return JSON.stringify([stompFrame]);
}

function stompFrame(lines, body = "") {
  return `${lines.join("\n")}\n\n${body}\0`;
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

export function connect_ws(roomId, dispatch) {
  if (
    socket &&
    (socket.readyState === WebSocket.CONNECTING ||
      socket.readyState === WebSocket.OPEN)
  ) {
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
        continue;
      }

      if (frame.startsWith("MESSAGE")) {
        const parsed = parseStompChatMessage(frame);
        if (parsed !== null) dispatch(parsed);
      }
    }
  };

  connection.onclose = () => {
    if (socket === connection) socket = null;
  };
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
