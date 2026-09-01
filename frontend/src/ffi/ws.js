let socket = null;

export function connect_ws(url_path, dispatch) {
  if (
    socket &&
    (socket.readyState === WebSocket.CONNECTING ||
      socket.readyState === WebSocket.OPEN)
  ) {
    return;
  }

  const url = new URL(url_path, window.location.href);
  url.protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const connection = new WebSocket(url);
  socket = connection;
  
  connection.onmessage = (event) => {
    dispatch(event.data);
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
