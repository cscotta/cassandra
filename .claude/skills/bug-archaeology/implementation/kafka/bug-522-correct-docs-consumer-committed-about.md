**Commit**: 9f03f5b8a4 · **Ticket**: KAFKA-19993
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Updated the Javadoc for `Consumer#committed` to specify that null is
returned for   nonexistent partitions instead of throwing an exception. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * If any of the partitions requested do not exist, an exception would be thrown.
     * If any of the partitions requested do not exist, an exception would be thrown.

// After
    @ClusterTest
    public void testClassicConsumerNoCommittedOffsets() {
        testNoCommittedOffsets(GroupProtocol.CLASSIC);
    }

    @ClusterTest
    public void testAsyncConsumerNoCommittedOffsets() {
        testNoCommittedOffsets(GroupProtocol.CONSUMER);
    }

    private void testNoCommittedOffsets(GroupProtocol groupProtocol) {
        try (var consumer = createConsumer(groupProtocol, true)) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19993 correct the docs of consumer#committed about the nonexistent partition (#21196).

**Test**
Test modified in PlaintextConsumerCommitTest.java covering the corrected behaviour.
