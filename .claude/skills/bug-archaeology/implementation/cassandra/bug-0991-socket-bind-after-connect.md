# Bug 0991: OutboundTcpConnectionPool Binds Socket After Connect

**Commit**: a55fd76ddd · **Ticket**: CASSANDRA-9309
**Subsystem**: Internode messaging / Network
**Category**: Logic error in condition

---

## Root Cause
`OutboundTcpConnectionPool.newSocket` opened a connected socket with `SocketChannel.open(new InetSocketAddress(...))` (which both creates and connects in one call) and then attempted to `bind` a local address on the already-connected socket. Binding after connect is not supported; the socket already has a local address assigned by the OS. The fix separates the two steps: create the channel, optionally bind the local address, then connect — so `Config.getOutboundBindAny()` actually controls which local interface is used.

### Before / After
```java
// BEFORE — bind after connect; always ignored
Socket socket = SocketChannel.open(new InetSocketAddress(endpoint, port)).socket();
if (Config.getOutboundBindAny() && !socket.isBound())
    socket.bind(new InetSocketAddress(FBUtilities.getLocalAddress(), 0));
return socket;

// AFTER — bind before connect
SocketChannel channel = SocketChannel.open();
if (!Config.getOutboundBindAny())
    channel.bind(new InetSocketAddress(FBUtilities.getLocalAddress(), 0));
channel.connect(new InetSocketAddress(endpoint, port));
return channel.socket();
```

---

## How to Spot
Any code that calls `SocketChannel.open(SocketAddress)` (the convenience overload that connects immediately) and then attempts to bind the resulting socket is logically wrong. Use the no-argument `SocketChannel.open()` form when you need explicit control over the local bind address.

---

## Test
No test added.
