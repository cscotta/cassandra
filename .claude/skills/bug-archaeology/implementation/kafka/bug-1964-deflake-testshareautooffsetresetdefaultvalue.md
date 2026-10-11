**Commit**: 48d60efe98 · **Ticket**: KAFKA-17990
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
ShareConsumerTest.testShareAutoOffsetResetDefaultValue has been tightened up by making sure that records produced have been flushed before starting consumption. A possible but unlikely race condition seems the source of the flakiness and this should now be eliminated in the previous PR to this test case. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgeCommitCallback(partitionOffsetsMap, partitionExceptionMap));
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgeCommitCallback(partitionOffsetsMap, partitionExceptionMap));
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgeCommitCallback(partitionOffsetsMap, partitionExceptionMap));
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgeCommitCallback(partitionOffsetsMap, partitionExceptionMap));
    private static class TestableAcknowledgeCommitCallback implements AcknowledgementCommitCallback {
        public TestableAcknowledgeCommitCallback(Map<TopicPartition, Set<Long>> partitionOffsetsMap,
                                                 Map<TopicPartition, Exception> partitionExceptionMap) {
            shareConsumer1.setAcknowledgementCommitCallback(new TestableAcknowledgeCommitCallback(partitionOffsetsMap1, partitionExceptionMap1));
            shareConsumer1.setAcknowledgementCommitCallback(new TestableAcknowledgeCommitCallback(partitionOffsetsMap, partitionExceptionMap));

// After
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgementCommitCallback(partitionOffsetsMap, partitionExceptionMap));
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgementCommitCallback(partitionOffsetsMap, partitionExceptionMap));
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgementCommitCallback(partitionOffsetsMap, partitionExceptionMap));
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgementCommitCallback(partitionOffsetsMap, partitionExceptionMap));
    private static class TestableAcknowledgementCommitCallback implements AcknowledgementCommitCallback {
        public TestableAcknowledgementCommitCallback(Map<TopicPartition, Set<Long>> partitionOffsetsMap,
                                                     Map<TopicPartition, Exception> partitionExceptionMap) {
            shareConsumer1.setAcknowledgementCommitCallback(new TestableAcknowledgementCommitCallback(partitionOffsetsMap1, partitionExceptionMap1));
            shareConsumer1.setAcknowledgementCommitCallback(new TestableAcknowledgementCommitCallback(partitionOffsetsMap, partitionExceptionMap));
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17990: deflake testshareautooffsetresetdefaultvalue (#17916).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
