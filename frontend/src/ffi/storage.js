const CURRENT_ROOM_ID_KEY = "online-workspace.current-room-id";

export function get_current_room_id() {
  return sessionStorage.getItem(CURRENT_ROOM_ID_KEY) || "";
}

export function set_current_room_id(roomId) {
  sessionStorage.setItem(CURRENT_ROOM_ID_KEY, roomId);
}

export function clear_current_room_id() {
  sessionStorage.removeItem(CURRENT_ROOM_ID_KEY);
}
