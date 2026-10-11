**Commit**: 126b25b51d · **Ticket**: KAFKA-17288
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Partition epochs are tracked for consumer groups where epoch is the current assigned member epoch. As share groups have partitions shared hence maintaing the partition epochs is not required. Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * The current partition epoch maps each topic-partitions to their current epoch where
     * the epoch is the epoch of their owners. When a member revokes a partition, it removes
     * its epochs from this map. When a member gets a partition, it adds its epochs to this map.
     */
    protected final TimelineHashMap<Uuid, TimelineHashMap<Integer, Integer>> currentPartitionEpoch;

        this.currentPartitionEpoch = new TimelineHashMap<>(snapshotRegistry, 0);
    /**
     * Returns the current epoch of a partition or -1 if the partition
     * does not have one.
     *

// After
// (no additions)
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17288 removed tracking partition member epoch (kip-932) (#16828).

**Test**
Test modified in ShareGroupTest.java covering the corrected behaviour.
