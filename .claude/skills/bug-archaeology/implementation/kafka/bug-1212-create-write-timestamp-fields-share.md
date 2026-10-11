**Commit**: c3b7aa6e64 · **Ticket**: KAFKA-18170
**Subsystem**: share-coordinator/src
**Category**: Logic error in condition

**Root cause**
* We wish to track the time of creation of the `ShareSnapshot` records
so that automated jobs could force their creation if a share partition
has gone cold (no updates for a specified time interval). * To accomplish this, we have added 2 new fields `CreateTimestamp` and
`WriteTimestamp` in the `ShareSnapshot` record. * The former tracks snapshot creation due to regular RPC calls while the
latter will track snapshots created by periodic jobs. * In this PR we have made the requisite changes.

**Before/After**
```java
// Before
        Some("{\"version\":\"0\",\"data\":{\"snapshotEpoch\":0,\"stateEpoch\":0,\"leaderEpoch\":0,\"startOffset\":0,\"stateBatches\":[{\"firstOffset\":0,\"lastOffset\":4,\"deliveryState\":2,\"deliveryCount\":1}]}}")
import java.util.stream.Collectors;
                    .collect(Collectors.toList())),
                    .collect(Collectors.toList())),

// After
    val timestamp = System.currentTimeMillis
        Some(s"{\"version\":\"0\",\"data\":{\"snapshotEpoch\":0,\"stateEpoch\":0,\"leaderEpoch\":0,\"startOffset\":0,\"createTimestamp\":$timestamp,\"writeTimestamp\":$timestamp,\"stateBatches\":[{\"firstOffset\":0,\"lastOffset\":4,\"deliveryState\":2,\"deliveryCount\":1}]}}")
          .setCreateTimestamp(timestamp)
          .setWriteTimestamp(timestamp)
                    .toList())
                .setCreateTimestamp(offsetData.createTimestamp())
                .setWriteTimestamp(offsetData.writeTimestamp()),
                    .toList()),
    private final Time time;
        private Time time;
```

**How to spot**
When reviewing share-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18170: add create and write timestamp fields in share snapshot [1/n] (#19432).

**Test**
Test modified in DumpLogSegmentsTest.scala covering the corrected behaviour.
