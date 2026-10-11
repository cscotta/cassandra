**Commit**: d067c6c040 · **Ticket**: KAFKA-19716
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
When nested Timeline collections are created and discarded while loading
a coordinator partition, references to them accumulate in the current
snapshot. Allow the GC to reclaim them by starting a new snapshot and
discarding previous snapshots every 16,384 records. Small intervals degrade loading times for non-transactional offset
commit workloads while large intervals degrade loading times for
transactional workloads. A default of 16,384 was chosen as a compromise.

**Before/After**
```java
// Before
    <allow class="org.apache.kafka.coordinator.common.runtime.CoordinatorMetadataImage"/>
    <allow class="org.apache.kafka.coordinator.common.runtime.KRaftCoordinatorMetadataImage"/>
    <allow class="org.apache.kafka.coordinator.common.runtime.HdrHistogram"/>
        int loadBufferSize
            long previousHighWatermark = -1L;
                ReplayResult replayResult = processMemoryRecords(tp, log, memoryRecords, coordinator, stats, currentOffset, previousHighWatermark);

// After
    implementation project(':coordinator-common').sourceSets.test.output
    <allow pkg="org.apache.kafka.coordinator.common.runtime"/>
    /**
     * The interval between updating the last committed offset during loading, in offsets. Smaller
     * values commit more often at the expense of loading times when the workload is simple and does
     * not create collections that need to participate in {@link CoordinatorPlayback} snapshotting.
     * Larger values commit less often and allow more temporary data to accumulate before the next
     * commit when the workload creates many temporary collections that need to be snapshotted.
     *
     * The value of 16,384 was chosen as a trade-off between the performance of these two workloads.
     *
     * When changing this value, please run the GroupCoordinatorShardLoadingBenchmark to evaluate
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19716: clear out coordinator snapshots periodically while loading (#20547).

**Test**
Test modified in CoordinatorLoaderImplTest.java covering the corrected behaviour.
