**Commit**: 6fe1598e6b · **Ticket**: KAFKA-18170
**Subsystem**: share-coordinator/src
**Category**: Logic error in condition

**Root cause**
* There could be scenarios where share partition records in
`__share_group_state` internal topic are not updated for a while
implying these partitions are basically cold. * In this situation, the presence of these holds back the
pruner from keeping the topic clean and of manageable size. * To remedy the situation, we have added a periodic
`setupSnapshotColdPartitions` in `ShareCoordinatorService` which does a
writeAll operation on the associated shards in the coordinator and
forces snapshot creation for any cold partitions. In this way the pruner
can continue.

**Before/After**
```java
// Before
        .defineInternal(STATE_TOPIC_PRUNE_INTERVAL_MS_CONFIG, INT, STATE_TOPIC_PRUNE_INTERVAL_MS_DEFAULT, atLeast(1), LOW, STATE_TOPIC_PRUNE_INTERVAL_MS_DOC);

        setupRecordPruning();

// After
    public static final String COLD_PARTITION_SNAPSHOT_INTERVAL_MS_CONFIG = "share.coordinator.cold.partition.snapshot.interval.ms";
    public static final int COLD_PARTITION_SNAPSHOT_INTERVAL_MS_DEFAULT = 5 * 60 * 1000; // 5 minutes
    public static final String COLD_PARTITION_SNAPSHOT_INTERVAL_MS_DOC = "The duration in milliseconds that the share coordinator will wait between force snapshotting share partitions which are not being updated.";

        .defineInternal(STATE_TOPIC_PRUNE_INTERVAL_MS_CONFIG, INT, STATE_TOPIC_PRUNE_INTERVAL_MS_DEFAULT, atLeast(1), LOW, STATE_TOPIC_PRUNE_INTERVAL_MS_DOC)
        .defineInternal(COLD_PARTITION_SNAPSHOT_INTERVAL_MS_CONFIG, INT, COLD_PARTITION_SNAPSHOT_INTERVAL_MS_DEFAULT, atLeast(1), LOW, COLD_PARTITION_SNAPSHOT_INTERVAL_MS_DOC);
    private final int coldPartitionSnapshotIntervalMs;
        coldPartitionSnapshotIntervalMs = config.getInt(COLD_PARTITION_SNAPSHOT_INTERVAL_MS_CONFIG);
    public int shareCoordinatorColdPartitionSnapshotIntervalMs() {
        return coldPartitionSnapshotIntervalMs;
    }

```

**How to spot**
When reviewing share-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18170: add scheduled job to snapshot cold share partitions. (#19443).

**Test**
Test modified in ShareCoordinatorServiceTest.java covering the corrected behaviour.
