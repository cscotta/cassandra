**Commit**: e7859bbabe · **Ticket**: KAFKA-18274
**Subsystem**: test-common/src
**Category**: Resource leak

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After

                if (socketChannel.isOpen()) {
                    return socketChannel;
                }
                // When restarting components(e.g. controllers, brokers) in tests, we want to reuse the same
                // port that was previously allocated to maintain consistent addressing
                // so the client can reconnect to the same port.
                // Since those components would close the socket when they are restarted,
                // we need to rebind the socket to the same port.
                socketAddress = new InetSocketAddress(socketAddress.getHostString(), socketChannel.socket().getLocalPort());
                socketChannel = ServerSocketFactory.INSTANCE.openServerSocket(
                        listenerName,
```

**How to spot**
When reviewing test-common/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18274 failed to restart controller in testing due to closed socket channel [2/2] (#18337).

**Test**
Test modified in PreboundSocketFactoryManager.java covering the corrected behaviour.
