**Commit**: 7d91bc8cf9 · **Ticket**: KAFKA-17046
**Subsystem**: LICENSE-binary
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
netty-buffer-4.1.100.Final
netty-codec-4.1.100.Final
netty-common-4.1.100.Final
netty-handler-4.1.100.Final
netty-resolver-4.1.100.Final
netty-transport-4.1.100.Final
netty-transport-classes-epoll-4.1.100.Final
netty-transport-native-epoll-4.1.100.Final
netty-transport-native-unix-common-4.1.100.Final
  netty: "4.1.110.Final",

// After
netty-buffer-4.1.111.Final
netty-codec-4.1.111.Final
netty-common-4.1.111.Final
netty-handler-4.1.111.Final
netty-resolver-4.1.111.Final
netty-transport-4.1.111.Final
netty-transport-classes-epoll-4.1.111.Final
netty-transport-native-epoll-4.1.111.Final
netty-transport-native-unix-common-4.1.111.Final
  netty: "4.1.111.Final",
```

**How to spot**
When reviewing LICENSE-binary code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17046 upgrade netty version to 4.1.111.final (#16469).

**Test**
No test added.
