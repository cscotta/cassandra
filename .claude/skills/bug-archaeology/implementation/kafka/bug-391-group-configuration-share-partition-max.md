**Commit**: 37c0b9debe · **Ticket**: KAFKA-20160
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add group-level dynamic config `share.partition.max.record.locks`
(KIP-1240) to control the maximum number of record locks per
share-partition, with broker-level min/max bounds validation. Add `partitionMaxRecordLocksOrDefault` to `ShareGroupConfigProvider`
introduced in #21467. Consolidate duplicate `alterShare*` test helpers (including
`alterShareRenewAcknowledgeEnable` from #21467) into a generic
`alterShareGroupConfig` method. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.admin.Config;
import static org.junit.jupiter.api.Assertions.assertNotNull;
        @ClusterConfigProperty(key = "group.share.max.partition.max.record.locks", value = "10000"),
            @ClusterConfigProperty(key = "group.share.max.partition.max.record.locks", value = "10000"),
            @ClusterConfigProperty(key = "group.share.max.partition.max.record.locks", value = "10000"),
            @ClusterConfigProperty(key = "group.share.max.partition.max.record.locks", value = "10000"),
    @ClusterTest
    public void testDynamicPartitionMaxRecordLocks() {
        // Verify that the group-level share.partition.max.record.locks config can be
        // dynamically set and read back via describe configs.
        alterSharePartitionMaxRecordLocks("group1", "500");

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20160: group configuration for share.partition.max.record.locks (#21678).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
