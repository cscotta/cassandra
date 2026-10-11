**Commit**: a07294a732 · **Ticket**: KAFKA-17204
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
To avoid a resource leak, we need to close the AdminClient after the test. Reviewers: Lianet Magrans <lianetmr@gmail.com>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
        Utils.closeQuietly(adminClient, "admin");
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17204: kafkastreamscloseoptionsintegrationtest.before leaks adminclient (#16692).

**Test**
Test modified in KafkaStreamsCloseOptionsIntegrationTest.java covering the corrected behaviour.
