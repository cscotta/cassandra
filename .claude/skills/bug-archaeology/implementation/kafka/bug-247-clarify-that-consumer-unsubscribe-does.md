**Commit**: 40e9fcd742 · **Ticket**: KAFKA-20119
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Document that unsubscribe() doesn't commit offsets even with auto-commit
enabled,  and add test to verify this behavior

Reviewers: Lianet Magrans <lmagrans@confluent.io>, David Jacot
 <djacot@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    @ClusterTest
    public void testClassicConsumerUnsubscribeDoesNotCommitOffsetsWithAutoCommitEnabled() throws Exception {
        testUnsubscribeDoesNotCommitOffsetsWithAutoCommitEnabled(GroupProtocol.CLASSIC);
    }

    @ClusterTest
    public void testAsyncConsumerUnsubscribeDoesNotCommitOffsetsWithAutoCommitEnabled() throws Exception {
        testUnsubscribeDoesNotCommitOffsetsWithAutoCommitEnabled(GroupProtocol.CONSUMER);
    }

    /**
     * Verify that {@link Consumer#unsubscribe()} does not commit offsets even when
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20119 clarify that `consumer#unsubscribe` does not trigger auto-commit (#21424).

**Test**
Test modified in PlaintextConsumerTest.java covering the corrected behaviour.
