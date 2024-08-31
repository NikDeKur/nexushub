# Protocol

1. Client connects to server
2. Server sends to client PacketHello
3. Client responds to server PacketAuth
4. If auth is successful, server sends to client PacketReady, else closes connection
5. Client may send any packets to server.