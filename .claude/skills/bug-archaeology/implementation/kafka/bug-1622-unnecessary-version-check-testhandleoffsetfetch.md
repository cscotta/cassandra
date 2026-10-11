**Commit**: 60d08a7abb · **Ticket**: KAFKA-18552
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
    // Version 0 gets offsets from Zookeeper. We are not interested
    // in testing this here.
    if (version == 0) return

    // Version 0 gets offsets from Zookeeper. We are not interested
    // in testing this here.
    if (version == 0) return


// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18552: remove unnecessary version check in testhandleoffsetfetch* (#18559).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
