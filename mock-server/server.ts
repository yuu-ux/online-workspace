Deno.serve({ port: 8081 }, (req) => {
  if (req.headers.get("upgrade") != "websocket") {
    return new Response(null, { status: 501 });
  }
  
  const { socket, response } = Deno.upgradeWebSocket(req);
  let intervalId: number; // タイマーのIDを保持する変数

  socket.onopen = () => {
    console.log("🟢 接続されました");
    
    // 接続時にタイマーをセット: 3秒 (3000ms) ごとにメッセージを送信
    let counter = 1;
    intervalId = setInterval(() => {
      // 本番のJSONを想定したダミーデータ
      const dummyData = {
        type: "chat_message",
        room_id: 1,
        user: "Test",
        message: `定期メッセージ ${counter} 回目です！`
      };
      
      // JSON文字列にして送信
      socket.send(JSON.stringify(dummyData));
      console.log(`📤 定期送信: ${counter} 回目`);
      
      counter++;
    }, 3000);
  };

  socket.onmessage = (e) => {
    console.log(`📩 受信: ${e.data}`);
    // クライアントからのメッセージに対するオウム返しも残しておきます
    socket.send(JSON.stringify({
      type: "chat_message",
      room_id: 1,
      user: "EchoBot",
      message: `「${e.data}」ですね！`
    }));
  };

  socket.onclose = () => {
    console.log("🔴 切断されました");
    // ⭐️ 接続が切れたらタイマーを必ず解除する（メモリリーク防止）
    clearInterval(intervalId);
  };

  return response;
});
