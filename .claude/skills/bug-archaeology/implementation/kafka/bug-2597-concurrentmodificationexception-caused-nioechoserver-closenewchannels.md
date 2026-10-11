**Commit**: e7317b37bf · **Ticket**: KAFKA-17269
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        for (SocketChannel channel : socketChannels) {
            channel.close();
        socketChannels.clear();
        for (SocketChannel channel : newChannels) {
            channel.close();
        newChannels.clear();

// After
        synchronized (socketChannels) {
            for (SocketChannel channel : socketChannels) {
                channel.close();
            }
            socketChannels.clear();
        synchronized (newChannels) {
            for (SocketChannel channel : newChannels) {
                channel.close();
            }
            newChannels.clear();
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17269 fix concurrentmodificationexception caused by nioechoserver.closenewchannels (#16817).

**Test**
Test modified in NioEchoServer.java covering the corrected behaviour.
