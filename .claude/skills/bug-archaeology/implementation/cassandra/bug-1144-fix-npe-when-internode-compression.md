# Bug 1144: Fix NPE when internode compression is off

**Commit**: ed4fad19ec · **Ticket**: CASSANDRA-9884
**Subsystem**: src/java/org/apache/cassandra/net/IncomingTcpConnection.java
**Category**: Missing null / bounds check

---

## Root Cause

`IncomingTcpConnection` unconditionally called `socket.getChannel()` and passed the result directly to `NIODataInputStream`. When internode compression is off, the socket may be backed by a plain `Socket` that has no associated `SocketChannel`, so `socket.getChannel()` returns `null`. `NIODataInputStream` does not accept a null channel, causing a `NullPointerException` before any data is read. The assumption that every connected `Socket` has an associated NIO `SocketChannel` only holds for sockets created through `ServerSocketChannel.accept()`; sockets created by other means (e.g., wrapping a plain stream) legitimately return `null`.

### Before / After

```java
// BEFORE
in = new NIODataInputStream(socket.getChannel(), BUFFER_SIZE);
```

```java
// AFTER
ReadableByteChannel channel = socket.getChannel();
in = new NIODataInputStream(channel != null ? channel : Channels.newChannel(socket.getInputStream()), BUFFER_SIZE);
```

---

## How to Spot

Whenever code calls `Socket.getChannel()` and immediately uses the return value without a null check, flag it: the NIO channel is optional on `Socket` and is only present for sockets obtained from `ServerSocketChannel` or `SocketChannel.open()`. A reviewer should require either a null guard or a comment justifying why null is impossible in that context.

---

## Test

No test added.
