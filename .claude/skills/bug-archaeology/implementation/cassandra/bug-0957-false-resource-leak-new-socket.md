# Bug 0957: False resource-leak warning in OutboundTcpConnectionPool.newSocket

**Commit**: f3b5ff8422 · **Ticket**: CASSANDRA-11083
**Subsystem**: Networking
**Category**: Resource leak

---

## Root Cause
`OutboundTcpConnectionPool.newSocket(InetAddress)` opened a `SocketChannel` and wrapped it in a `Socket`, then returned the socket to the caller. Static analysis tools reported this as a potential resource leak because the channel object appears to escape without being closed by the method itself. In reality, closing the returned socket closes the underlying channel, so no real leak exists. The fix adds a `@SuppressWarnings("resource")` annotation and a comment documenting this ownership relationship, suppressing the false positive without altering runtime behaviour.

### Before / After
```java
// BEFORE
public static Socket newSocket(InetAddress endpoint) throws IOException {
    // zero means 'bind on any available port.'
    ...
}

// AFTER
// Closing the socket will close the underlying channel.
@SuppressWarnings("resource")
public static Socket newSocket(InetAddress endpoint) throws IOException {
    ...
}
```

---

## How to Spot
This is the same pattern as CASSANDRA-11084 applied to the same `OutboundTcpConnectionPool.newSocket` method; commit 0956 added the annotation to the two-argument overload in a separate commit. When reviewing NIO code that wraps a `SocketChannel` in a `Socket.getChannel()`-based adapter, verify that all paths through the factory either close on error or document transitive ownership before suppressing leak warnings.

---

## Test
No test added.
