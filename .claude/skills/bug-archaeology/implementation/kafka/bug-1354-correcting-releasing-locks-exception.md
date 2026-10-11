**Commit**: c1fc59fc23 · **Ticket**: KAFKA-18918
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The PR corrects the way the locks are released on exception. As
`partitionsAcquired` can be a reference to `topicPartitionData`, hence
the locks should released prior clearing `partitionsAcquired`. Reviewers: Abhinav Dixit <adixit@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            releasePartitionLocks(topicPartitionData.keySet());

// After
            releasePartitionLocks(topicPartitionData.keySet());
import static org.mockito.Mockito.doThrow;
        Mockito.verify(sp0, times(1)).releaseFetchLock();
    @Test
    public void testTryCompleteLocksReleasedOnCompleteException() {
        ReplicaManager replicaManager = mock(ReplicaManager.class);
        TopicIdPartition tp0 = new TopicIdPartition(Uuid.randomUuid(), new TopicPartition("foo", 0));
        LinkedHashMap<TopicIdPartition, Integer> partitionMaxBytes = orderedMap(PARTITION_MAX_BYTES, tp0);

        SharePartition sp0 = mock(SharePartition.class);
        when(sp0.maybeAcquireFetchLock()).thenReturn(true);
        when(sp0.canAcquireRecords()).thenReturn(true);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18918: correcting releasing of locks on exception (#19091).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
