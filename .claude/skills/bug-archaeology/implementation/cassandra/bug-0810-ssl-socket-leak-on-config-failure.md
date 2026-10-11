# Bug 0810: SSL socket/server-socket leaked when cipher-suite configuration throws

**Commit**: 86a73d439c · **Ticket**: CASSANDRA-12329
**Subsystem**: Security / SSL
**Category**: Resource leak

---

## Root Cause
`SSLFactory` created SSL sockets and server sockets and then immediately configured cipher suites and protocols on them. If `filterCipherSuites()` threw an `IllegalArgumentException` (e.g. an invalid cipher name), or if `bind()` threw on the server socket, the already-created socket was abandoned without being closed. Four factory methods (`getServerSocket`, `getSocket` with various signatures) all had this pattern. The fix wraps the configuration block in a `try`/`catch` that explicitly closes the socket before re-throwing.

### Before / After
```java
// BEFORE (getServerSocket example)
SSLServerSocket serverSocket = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket();
serverSocket.setReuseAddress(true);
String[] suites = filterCipherSuites(...);   // throws IllegalArgumentException on bad config
serverSocket.setEnabledCipherSuites(suites);
serverSocket.bind(...);
return serverSocket;

// AFTER
SSLServerSocket serverSocket = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket();
try {
    serverSocket.setReuseAddress(true);
    String[] suites = filterCipherSuites(...);
    serverSocket.setEnabledCipherSuites(suites);
    serverSocket.bind(...);
    return serverSocket;
} catch (IllegalArgumentException | SecurityException | IOException e) {
    serverSocket.close();
    throw e;
}
```

---

## How to Spot
Any code that creates a network resource and then performs further configuration (cipher suites, protocols, bind addresses) without a `finally` or try-with-resources guard leaks the resource on configuration failure. Audit every `createSocket()`/`createServerSocket()` call: if the configuration happens outside a catch that closes the socket, it is a leak.

---

## Test
No new test added. The fix is purely defensive cleanup around existing socket-creation paths.
