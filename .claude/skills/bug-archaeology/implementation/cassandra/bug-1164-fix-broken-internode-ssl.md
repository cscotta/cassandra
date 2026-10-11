# Bug 1164: Broken Internode SSL Due to Null SocketChannel

**Commit**: 33e3af59f9 · **Ticket**: CASSANDRA-9884
**Subsystem**: net/OutboundTcpConnection.java
**Category**: Missing null / bounds check

---

## Root Cause

`OutboundTcpConnection.connect()` obtained a `WritableByteChannel` by calling `socket.getChannel()` and passed it directly to `BufferedDataOutputStreamPlus`. For plain TCP sockets, `Socket.getChannel()` returns the underlying `SocketChannel`. However, when TLS is in use, the `SSLSocket` returned by the SSL engine wraps the real socket but does not expose an NIO channel; `getChannel()` returns `null`. Passing `null` to `BufferedDataOutputStreamPlus` caused a `NullPointerException` and broke all encrypted internode communication.

### Before / After

```java
// BEFORE
out = new BufferedDataOutputStreamPlus(socket.getChannel(), BUFFER_SIZE);
```

```java
// AFTER
// SocketChannel may be null when using SSL
WritableByteChannel ch = socket.getChannel();
out = new BufferedDataOutputStreamPlus(ch != null ? ch : Channels.newChannel(socket.getOutputStream()), BUFFER_SIZE);
```

---

## How to Spot

Any call to `Socket.getChannel()` that is used without a null check is suspect: the NIO channel is only available when the socket was created through `ServerSocketChannel.accept()` or `SocketChannel.open()`, and SSL wrappers do not propagate it. Reviewers should look for code that assumes `getChannel()` is non-null and ask whether the code path can be reached via an SSL socket; if so, a fallback via `Channels.newChannel(socket.getOutputStream())` is the standard remedy.

---

## Test

No test added.
