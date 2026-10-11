**Commit**: 3a9777a667 · **Ticket**: KAFKA-14619
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When KafkaRaftClient receives a request to create a snapshot with end offset that is not aligned to a batch boundary, do not create a misaligned snapshot and instead log at info level and throw an IllegalArgumentException. Checking that the end offset is at a batch boundary is performed by reading the log at snapshotId.offset() and checking whether the offset in question is the base offset of the returned batch. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
    log.createNewSnapshot(new OffsetAndEpoch(numberOfRecords - 1, firstEpoch)).get().close()
    log.createNewSnapshot(new OffsetAndEpoch(1, firstEpoch)).get().close()
    log.createNewSnapshot(new OffsetAndEpoch(2 * numberOfRecords - 1, secondEpoch)).get().close()
    log.createNewSnapshot(new OffsetAndEpoch(numberOfRecords + 1, secondEpoch)).get().close()
    append(log, numberOfRecords, epoch)
    val snapshotId = new OffsetAndEpoch(numberOfRecords - 1, epoch)
        assertThrows(IllegalArgumentException.class, () -> context.client.createSnapshot(invalidSnapshotId1, 0));
        OffsetAndEpoch invalidSnapshotId2 = new OffsetAndEpoch(context.client.highWatermark().getAsLong() + 2, c

// After
    /*
      Perform a check that the requested snapshot offset is batch aligned via a log read, which
      returns the base offset of the batch that contains the requested offset. A snapshot offset
      is one greater than the last offset contained in the snapshot, and cannot go past the high
      watermark.

      This check is necessary because Raft replication code assumes the snapshot offset is the
      start of a batch. If a follower applies a non-batch aligned snapshot at offset (X) and
      fetches from this offset, the returned batch will start at offset (X - M), and the
      follower will be unable to append it since (X - M) < (X).
     */
    val baseOffset = read(snapshotId.offset, Isolation.COMMITTED).startOffsetMetadata.offset
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14619; kraft validate snapshot id are at batch boundaries (#17500).

**Test**
Test modified in KafkaMetadataLogTest.scala covering the corrected behaviour.
