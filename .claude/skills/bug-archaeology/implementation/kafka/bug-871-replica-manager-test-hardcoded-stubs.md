**Commit**: 7e9df7d03d · **Ticket**: KAFKA-19505

**Subsystem**: Replication / ReplicaManagerTest

**Category**: Flaky test

**Root cause**
`ReplicaManagerTest.setupMockLog` stubbed `topicId` and `topicPartition` with hard-coded values. When a test exercised multiple partitions, all of them shared the same stubs, causing unexpected mock interactions and assertion failures depending on invocation order. The fix parameterizes `setupMockLog` to accept `topicPartition` and `topicId` arguments so each mock log is configured independently.

**Before/After**
```java
// Before: hard-coded partition/topicId in helper
private UnifiedLog setupMockLog() {
    UnifiedLog log = mock(UnifiedLog.class);
    when(log.topicPartition()).thenReturn(new TopicPartition("topic", 0));
    when(log.topicId()).thenReturn(Optional.of(Uuid.ZERO_UUID));
    return log;
}

// After: parameterized helper
private UnifiedLog setupMockLog(TopicPartition tp, Uuid topicId) {
    UnifiedLog log = mock(UnifiedLog.class);
    when(log.topicPartition()).thenReturn(tp);
    when(log.topicId()).thenReturn(Optional.of(topicId));
    return log;
}
```

**How to spot**
Test helper methods that return mocks with hard-coded identifiers are fragile as soon as more than one instance is needed per test. Always parameterize stubs that encode entity identity (partition, topic ID, broker ID).

**Test**
`ReplicaManagerTest` multi-partition tests now pass reliably with distinct mocks for each partition.
