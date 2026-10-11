**Commit**: 0548f1bc82 · **Ticket**: KAFKA-19017
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
In an earlier PR, the flag --consumer.config in
VerifiableShareConsumer.java was changed to --command-config. This PR
makes the same change when VerifiableShareConsumer is used in
verifiable_share_consumer.py. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        cmd += " --consumer.config %s" % VerifiableShareConsumer.CONFIG_FILE

// After
        cmd += " --command-config %s" % VerifiableShareConsumer.CONFIG_FILE
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19017: changed consumer.config to command-config in verifiable_share_consumer.py (#19254).

**Test**
Test modified in verifiable_share_consumer.py covering the corrected behaviour.
