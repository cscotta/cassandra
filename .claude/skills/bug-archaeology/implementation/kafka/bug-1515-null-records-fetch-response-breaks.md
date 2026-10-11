**Commit**: ca5d2cf76d · **Ticket**: KAFKA-18646
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Ensure we always return empty records (including cases where an error is returned). We also remove `nullable` from `records` since it is effectively expected to be
non-null by a large percentage of clients in the wild. This behavior regressed in fe56fc9 (KAFKA-18269). Empty records were
previously set via `FetchResponse.recordsOrFail(partitionData)` in the
now-removed `maybeConvertFetchedData` method.

**Before/After**
```java
// Before
            .setHighWatermark(FetchResponse.INVALID_HIGH_WATERMARK);
}
        { "name": "Records", "type": "records", "versions": "0+", "nullableVersions": "0+", "about": "The record data."}
                        .setAbortedTransactions(abortedTransactions));
                        .setAbortedTransactions(abortedTransactions));
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeTransactionsRequestData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, ProduceRequestData, SyncGroupRequestData, WriteTxnMarkersRequestData}

// After
            .setHighWatermark(FetchResponse.INVALID_HIGH_WATERMARK)
            .setRecords(MemoryRecords.EMPTY);
}
        { "name": "Records", "type": "records", "versions": "0+", "about": "The record data."}
                        .setAbortedTransactions(abortedTransactions)
                        .setRecords(MemoryRecords.EMPTY));
                        .setAbortedTransactions(abortedTransactions)
                        .setRecords(MemoryRecords.EMPTY));
im
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18646: null records in fetch response breaks librdkafka (#18726).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
