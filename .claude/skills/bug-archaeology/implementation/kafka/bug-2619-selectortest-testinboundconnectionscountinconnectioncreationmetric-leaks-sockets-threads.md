**Commit**: 736bd3cd09 · **Ticket**: KAFKA-17201
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                SocketChannel channel = ss.accept();
                channel.configureBlocking(false);

                selector.register(Integer.toString(i), channel);

// After
                try (SocketChannel channel = ss.accept()) {
                    channel.configureBlocking(false);
                    selector.register(Integer.toString(i), channel);
                } finally {
                    sender.join();
                }
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17201 selectortest.testinboundconnectionscountinconnectioncreationmetric leaks sockets and threads (#16723).

**Test**
Test modified in SelectorTest.java covering the corrected behaviour.
