**Commit**: f2ae20a5c1 · **Ticket**: KAFKA-18316
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    val props = TestUtils.createBrokerConfig(0, TestUtils.MockZkConnect, port = 0)
    props.put(SocketServerConfigs.LISTENER_SECURITY_PROTOCOL_MAP_CONFIG, "EXTERNAL:PLAINTEXT,REPLICATION:PLAINTEXT,ADMIN:PLAINTEXT")

// After
    val props = TestUtils.createBrokerConfig(0, null, port = 0)
    props.put(SocketServerConfigs.ADVERTISED_LISTENERS_CONFIG, "REPLICATION://localhost:1")
    props.put(SocketServerConfigs.LISTENER_SECURITY_PROTOCOL_MAP_CONFIG, "PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT,REPLICATION:PLAINTEXT,ADMIN:PLAINTEXT")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18316 fix to kraft or remove tests associate with zk broker config in connectionquotastest (#18279).

**Test**
Test modified in ConnectionQuotasTest.scala covering the corrected behaviour.
