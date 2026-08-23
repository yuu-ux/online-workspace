let socket = null;

export function connect_ws(url_path, dispatch) {
  // console.log("JS: connect_ws is called! url_path:", url_path);

  if (socket) {
    console.log("JS: Socket already exists. Returning.");
    return;
  }

  const url = "ws://localhost:8081"; 
  // console.log("JS: Attempting to connect to:", url);
  
  socket = new WebSocket(url);
  
  socket.onopen = () => {
    // console.log("JS: 🟢 WebSocket Connected successfully!");
  };
  
  socket.onmessage = (event) => {
    // console.log("JS: 📩 Received data from server:", event.data);
    dispatch(event.data);
  };
  
  socket.onerror = (err) => {
    // console.error("JS: 🔴 WebSocket Error!", err);
  };

  socket.onclose = () => {
    // console.log("JS: 🔴 WebSocket Closed.");
  }
}

export function send_ws(message) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(message);
    // console.log("JS: Sent", message);
  } else {
    // console.warn("JS: Socket is not connected!");
  }
}

export function close_ws() {
  if (socket) {
    // console.log("JS: Closing WebSocket connection...");
    socket.close();
    socket = null; // 次の接続のためにリセットしておく
  }
}
