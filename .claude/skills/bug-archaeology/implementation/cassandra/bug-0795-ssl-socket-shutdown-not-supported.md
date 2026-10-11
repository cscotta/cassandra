# Bug 0795: Socket shutdownInput/shutdownOutput Crashes SSL Streaming

**Commit**: 78de13bcc7 · **Ticket**: CASSANDRA-12903
**Subsystem**: Streaming / SSL / Networking
**Category**: Logic error in condition

---

## Root Cause
`ConnectionHandler.initiateOnSendingSide()` and `initiateOnReceivingSide()` called `Socket.shutdownOutput()` on the incoming socket and `Socket.shutdownInput()` on the outgoing socket, intending to enforce one-directional use of each connection. However, `Socket.shutdownInput()` and `Socket.shutdownOutput()` are not supported on SSL/TLS sockets (`SSLSocket`) in the JDK — calling them throws `UnsupportedOperationException`, immediately crashing the streaming session when TLS internode encryption is enabled. Since plain and TLS connections use the same code path, the calls were simply removed: the directionality constraint they were meant to enforce is already maintained at the protocol level by the streaming handshake.

### Before / After
```java
// BEFORE
incoming.start(incomingSocket, StreamMessage.CURRENT_VERSION, true);
incomingSocket.shutdownOutput();   // crashes on SSLSocket

outgoing.start(outgoingSocket, StreamMessage.CURRENT_VERSION, true);
outgoingSocket.shutdownInput();    // crashes on SSLSocket

// AFTER
incoming.start(incomingSocket, StreamMessage.CURRENT_VERSION, true);
// no shutdownOutput

outgoing.start(outgoingSocket, StreamMessage.CURRENT_VERSION, true);
// no shutdownInput
```

---

## How to Spot
Any call to `Socket.shutdownInput()` or `Socket.shutdownOutput()` should be scrutinised for TLS compatibility. The JDK explicitly states that `SSLSocket` does not support half-close operations. Code that works in plaintext test environments can silently break when TLS is enabled in production. In review, ask: "Does this code path ever receive an `SSLSocket`?" If the answer is possibly yes, the half-close must be removed or guarded.

---

## Test
No test added.
