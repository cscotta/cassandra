**Commit**: d78ff06476 · **Ticket**: KAFKA-16967
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

                        String id = id(socketChannel);
    private String id(SocketChannel channel) {
        return channel.socket().getLocalAddress().getHostAddress() + ":" + channel.socket().getLocalPort() + "-" +
                channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort();

// After
import java.util.concurrent.atomic.AtomicLong;
    private final AtomicLong idGenerator = new AtomicLong();

                        String id = id();
    private String id() {
        return "connection-" + idGenerator.getAndIncrement();
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16967 nioechoserver fails to register connection and causes flaky failure. (#16384).

**Test**
Test modified in NioEchoServer.java covering the corrected behaviour.
