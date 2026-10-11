**Commit**: 09e3c12057 · **Ticket**: KAFKA-17543
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR tries to improve the error message when broker.id is set to -1 and ZK migration is enabled. It is not
needed to disable the broker.id.generation.enable option. It is sufficient to just not use it (by not setting
the broker.id to -1). Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
          require(brokerId != -1, "broker id generation is incompatible with migration to ZK. Please disable it before enabling migration")
          "broker broker.id.generation.enable is incompatible with migration to ZK. Please disable it before enabling migration")
      "requirement failed: broker id generation is incompatible with migration to ZK. Please disable it before enabling migration",

// After
          require(brokerId >= 0, "broker.id generation is incompatible with ZooKeeper migration. Please stop using it before enabling migration (set broker.id to a value greater or equal to 0).")
          "broker.id generation is incompatible with ZooKeeper migration. Please stop using it before enabling migration (set broker.id to a value greater or equal to 0).")
      "requirement failed: broker.id generation is incompatible with ZooKeeper migration. Please stop using it before enabling migration (set broker.id to a value greater or equal to 0).",
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17543: improve and clarify the error message about generated broker ids in migration (#17210).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
