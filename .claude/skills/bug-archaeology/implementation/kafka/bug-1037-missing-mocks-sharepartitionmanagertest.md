**Commit**: 77aff85b3e · **Ticket**: KAFKA-19268
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Added missing mocks for SharePartitionManagerTests. Reviewers: Abhinav Dixit <adixit@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
        sharePartitionManager = SharePartitionManagerBuilder.builder()
            .withCache(cache)
            .build();
        doAnswer(invocation -> buildLogReadResult(topicIdPartitions)).when(mockReplicaManager).readFromLog(any(), any(), any(ReplicaQuota.class), anyBoolean());
            new FetchDataInfo(new LogOffsetMetadata(0, 0, 0), MemoryRecords.EMPTY),

// After
import org.apache.kafka.common.compress.Compression;
import org.apache.kafka.common.record.SimpleRecord;
        SharePartition sp0 = mock(SharePartition.class);
        SharePartition sp1 = mock(SharePartition.class);
        SharePartition sp2 = mock(SharePartition.class);

        when(sp0.releaseAcquiredRecords(ArgumentMatchers.eq(String.valueOf(memberId1)))).thenReturn(CompletableFuture.completedFuture(null));
        when(sp1.releaseAcquiredRecords(ArgumentMatchers.eq(String.valueOf(memberId1)))).thenReturn(CompletableFuture.completedFuture(null));
        when(sp2.releaseAcquiredRecords(ArgumentMatchers.eq(String.valueOf(memberId1)))).thenReturn(CompletableFuture.completedFuture(null));

        SharePartitionCache partitionCache = new SharePartitionCache();
        partitionCache.put(new SharePartitionKey(groupId, tp0), sp0);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19268 missing mocks for sharepartitionmanagertest tests (#19786).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
