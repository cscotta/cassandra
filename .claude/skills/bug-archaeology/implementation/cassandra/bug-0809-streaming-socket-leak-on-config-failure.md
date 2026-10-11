# Bug 0809: Socket leak in DefaultConnectionFactory on partial connect failure

**Commit**: 83cc2ed3af · **Ticket**: CASSANDRA-12330
**Subsystem**: Streaming
**Category**: Resource leak

---

## Root Cause
`DefaultConnectionFactory.createConnection()` opened a new `Socket` via `OutboundTcpConnectionPool.newSocket()`, then configured it (`setSoTimeout`, `setKeepAlive`). If either configuration call threw an `IOException`, the socket was abandoned without being closed. The variable was originally declared inside the `try` block, making it inaccessible to any `catch` handler. The fix hoists the variable outside the `try` block so that the `catch` clause can close it before re-throwing.

### Before / After
```java
// BEFORE
try {
    Socket socket = OutboundTcpConnectionPool.newSocket(peer);
    socket.setSoTimeout(...);
    socket.setKeepAlive(true);
    return socket;
} catch (IOException e) {
    if (++attempts >= MAX_CONNECT_ATTEMPTS) throw e;
}

// AFTER
Socket socket = null;
try {
    socket = OutboundTcpConnectionPool.newSocket(peer);
    socket.setSoTimeout(...);
    socket.setKeepAlive(true);
    return socket;
} catch (IOException e) {
    if (socket != null) {
        try { socket.close(); } catch (IOException ignore) {}
    }
    if (++attempts >= MAX_CONNECT_ATTEMPTS) throw e;
}
```

---

## How to Spot
Whenever a resource is allocated inside a `try` block and the variable is not visible in the `catch`/`finally` block, a leak path exists for every exception thrown between allocation and successful return. The fix pattern — declare the variable before the `try`, null-check before closing in `catch` — is standard; prefer try-with-resources where applicable.

---

## Test
No dedicated new test added; the fix is a straightforward defensive resource-close.
