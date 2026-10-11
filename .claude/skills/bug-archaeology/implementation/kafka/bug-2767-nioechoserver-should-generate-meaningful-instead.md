**Commit**: c97d4ce026 · **Ticket**: KAFKA-17032
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.concurrent.atomic.AtomicLong;
    private final AtomicLong idGenerator = new AtomicLong();
                        String id = id();
                        selector.register(id, socketChannel);
    private String id() {
        return "connection-" + idGenerator.getAndIncrement();
    val localHost = socket.getLocalAddress.getHostAddress
    val localPort = socket.getLocalPort
    val remoteHost = socket.getInetAddress.getHostAddress
    val remotePort = socket.getPort
    val connId = ConnectionId(localHost, localPort, remoteHost, remotePort, nextConnectionIndex).toString

// After
    /**
     * Generates a unique connection ID for the given socket.
     *
     * @param socket The socket for which the connection ID is to be generated.
     * @param connectionIndex The index to be used in the connection ID to ensure uniqueness.
     * @return A string representing the unique connection ID.
     */
    public static String generateConnectionId(Socket socket, int connectionIndex) {
        String localHost = socket.getLocalAddress().getHostAddress();
        int localPort = socket.getLocalPort();
        String remoteHost = socket.getInetAddress().getHostAddress();
        int remotePort = socket.getPort();
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17032 nioechoserver should generate meaningful id instead of incremental number (#16460).

**Test**
Test modified in NioEchoServer.java covering the corrected behaviour.
