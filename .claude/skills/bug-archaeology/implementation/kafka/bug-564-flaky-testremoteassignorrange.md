**Commit**: 275c944446 · **Ticket**: KAFKA-19956
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
This has been flaky, failing with "Topic does not exist" received in a
response to a commit. This could happen given that the test had the default auto-commit
enabled and was not explicitly closing the consumer (consumer.close
triggered as part of the test infra tear down, which does close with
timeout 0). Seems the topic could end up deleted before the auto-commit
on close was processed. Disabling auto-commit, not needed for the test.

**Before/After**
```java
// Before
// (no removals)

// After
    this.consumerConfig.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19956: fix flaky testremoteassignorrange (#21060).

**Test**
Test modified in PlaintextConsumerAssignorsTest.scala covering the corrected behaviour.
