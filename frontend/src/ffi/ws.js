let socket = null;
let messageDispatch = null;
let desiredRoomIds = [];
let desiredChatRoomId = null;
let desiredFriendPresence = false;
let notificationDispatch = null;
let desiredNotifications = false;
let activeSubscriptionIds = new Set();

function sockFrame(stompFrame) {
  return JSON.stringify([stompFrame]);
}

function stompFrame(lines, body = "") {
  return `${lines.join("\n")}\n\n${body}\0`;
}

function subscribeChat(connection, roomId) {
  const id = `sub-room-messages-${roomId}`;
  connection.send(
    sockFrame(
      stompFrame([
        "SUBSCRIBE",
        `id:${id}`,
        `destination:/user/queue/rooms/${roomId}/messages`,
        "ack:auto",
      ])
    )
  );
  activeSubscriptionIds.add(id);
}

function subscribePresence(connection, roomId) {
  const id = `sub-room-presence-${roomId}`;
  connection.send(
    sockFrame(
      stompFrame([
        "SUBSCRIBE",
        `id:${id}`,
        `destination:/topic/rooms/${roomId}/presence`,
        "ack:auto",
      ])
    )
  );
  activeSubscriptionIds.add(id);
}

function subscribeRoomCreated(connection) {
  const id = "sub-room-created";
  connection.send(
    sockFrame(
      stompFrame([
        "SUBSCRIBE",
        `id:${id}`,
        "destination:/topic/rooms",
        "ack:auto",
      ])
    )
  );
  activeSubscriptionIds.add(id);
}

function subscribeFriendPresence(connection) {
  const id = "sub-friend-presence";
  connection.send(
    sockFrame(
      stompFrame([
        "SUBSCRIBE",
        `id:${id}`,
        "destination:/user/queue/friends/presence",
        "ack:auto",
      ])
    )
  );
  activeSubscriptionIds.add(id);
}

function subscribeNotifications(connection) {
  const id = "sub-notifications";
  connection.send(
    sockFrame(
      stompFrame([
        "SUBSCRIBE",
        `id:${id}`,
        "destination:/user/queue/notifications",
        "ack:auto",
      ])
    )
  );
  activeSubscriptionIds.add(id);
}

function applySubscriptions(connection) {
  activeSubscriptionIds.forEach((id) => {
    connection.send(sockFrame(stompFrame(["UNSUBSCRIBE", `id:${id}`])));
  });
  activeSubscriptionIds.clear();

  if (desiredChatRoomId !== null) {
    subscribeChat(connection, desiredChatRoomId);
  } else if (desiredRoomIds.length > 0) {
    subscribeRoomCreated(connection);
  }
  desiredRoomIds.forEach((roomId) => subscribePresence(connection, roomId));
  if (desiredFriendPresence) subscribeFriendPresence(connection);
  if (desiredNotifications) subscribeNotifications(connection);
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
      sent_at: event.payload.sentAt || "",
      icon_url: event.payload.sender?.iconUrl || "",
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

function parseStompRoomMemberCountMessage(frame) {
  const separator = frame.indexOf("\n\n");
  if (separator < 0) return null;

  const bodyWithNull = frame.slice(separator + 2);
  const body = bodyWithNull.endsWith("\0")
    ? bodyWithNull.slice(0, -1)
    : bodyWithNull;

  try {
    const event = JSON.parse(body);
    if (
      event.type !== "room:member_count_changed" ||
      !event.payload
    ) {
      return null;
    }
    return JSON.stringify({
      type: "room:member_count_changed",
      room_id: event.payload.roomId,
      current_members: event.payload.currentMembers,
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

function parseStompFriendPresenceMessage(frame) {
  const separator = frame.indexOf("\n\n");
  if (separator < 0) return null;

  const bodyWithNull = frame.slice(separator + 2);
  const body = bodyWithNull.endsWith("\0")
    ? bodyWithNull.slice(0, -1)
    : bodyWithNull;

  try {
    const event = JSON.parse(body);
    if (event.type !== "friend:presence_changed" || !event.payload) return null;
    return JSON.stringify({
      type: "friend_presence",
      user_id: event.payload.userId,
      online: event.payload.online,
    });
  } catch (_) {
    return null;
  }
}

function parseStompNotificationMessage(frame) {
  const separator = frame.indexOf("\n\n");
  if (separator < 0) return null;

  const bodyWithNull = frame.slice(separator + 2);
  const body = bodyWithNull.endsWith("\0")
    ? bodyWithNull.slice(0, -1)
    : bodyWithNull;

  try {
    const event = JSON.parse(body);
    if (event.type !== "notification" || typeof event.message !== "string") return null;
    return JSON.stringify({ type: "notification", message: event.message });
  } catch (_) {
    return null;
  }
}

function ensureSocket() {
  if (
    socket &&
    (socket.readyState === WebSocket.CONNECTING ||
      socket.readyState === WebSocket.OPEN)
  ) {
    if (socket.readyState === WebSocket.OPEN) applySubscriptions(socket);
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
  activeSubscriptionIds.clear();

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
        applySubscriptions(connection);
        continue;
      }

      if (frame.startsWith("MESSAGE")) {
        const notification = parseStompNotificationMessage(frame);
        const parsed = notification === null ? parseStompChatMessage(frame) : null;
        const presence =
          notification === null && parsed === null ? parseStompPresenceMessage(frame) : null;
        const memberCount =
          notification === null && parsed === null && presence === null
            ? parseStompRoomMemberCountMessage(frame)
            : null;
        const roomCreated =
          notification === null && parsed === null && presence === null && memberCount === null
            ? parseStompRoomCreatedMessage(frame)
            : null;
        const friendPresence =
          notification === null &&
          parsed === null &&
          presence === null &&
          memberCount === null &&
          roomCreated === null
            ? parseStompFriendPresenceMessage(frame)
            : null;
        if (notification !== null && notificationDispatch !== null) {
          notificationDispatch(notification);
        }
        if (parsed !== null && messageDispatch !== null) messageDispatch(parsed);
        if (presence !== null && messageDispatch !== null) messageDispatch(presence);
        if (memberCount !== null && messageDispatch !== null) messageDispatch(memberCount);
        if (roomCreated !== null && messageDispatch !== null) messageDispatch(roomCreated);
        if (friendPresence !== null && messageDispatch !== null) messageDispatch(friendPresence);
      }
    }
  };

  connection.onclose = () => {
    if (socket === connection) socket = null;
  };
}

function open_ws(roomIds, chatRoomId, dispatch, friendPresence = false) {
  messageDispatch = dispatch;
  desiredRoomIds = [...new Set(roomIds)];
  desiredChatRoomId = chatRoomId;
  desiredFriendPresence = friendPresence;
  ensureSocket();
}

export function connect_ws(roomId, dispatch) {
  open_ws([roomId], roomId, dispatch);
}

export function connect_room_list(roomIdsJson, dispatch) {
  open_ws(JSON.parse(roomIdsJson), null, dispatch);
}

export function connect_friend_presence(dispatch) {
  open_ws([], null, dispatch, true);
}

export function connect_notifications(dispatch) {
  notificationDispatch = dispatch;
  desiredNotifications = true;
  ensureSocket();
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
  messageDispatch = null;
  desiredRoomIds = [];
  desiredChatRoomId = null;
  desiredFriendPresence = false;
  notificationDispatch = null;
  desiredNotifications = false;
  activeSubscriptionIds.clear();
}
