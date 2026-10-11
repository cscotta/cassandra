**Commit**: 568b9e8a6c · **Ticket**: KAFKA-17803
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Jose Sancio <jsancio@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
     * Search forward for the file position of the last offset that is greater than or equal to the target offset
     * and return its physical position and the size of the message (including log overhead) at the returned offset. If
     * no such offsets are found, return null.
                return new LogOffsetPosition(offset, batch.position(), batch.sizeInBytes());
    fetchFollower(partition, replicaId = follower2, fetchOffset = lastOffsetOfFirstBatch)
    assertEquals(lastOffsetOfFirstBatch, partition.log.get.highWatermark, "Expected leader's HW")
  private def assertNonEmptyFetch(log: UnifiedLog, offset: Long, isolation: FetchIsolation): Unit = {
    assertEquals(offset, readInfo.fetchOffsetMetadata.messageOffset)
  private def assertEmptyFetch(log: UnifiedLog, offset: Long, isolation: FetchIsolation): Unit = {
    assertEquals(offset, readInfo.fetchOffsetMetadata.messageOffset)
      assertNonEmptyFetch(log, offset, FetchIsolation.LOG_END)

// After
     * Search forward for the file position of the message batch whose last offset that is greater
     * than or equal to the target offset. If no such batch is found, return null.
     * @return the batch's base offset, its physical position, and its size (including log overhead)
                return new LogOffsetPosition(batch.baseOffset(), batch.position(), batch.sizeInBytes());
    fetchFollower(partition, replicaId = follower2, fetchOffset = lastOffsetOfFirstBatch + 1)
    assertEquals(lastOffsetOfFirstBatch + 1, partition.log.get.highWatermark, "Expected leader's HW")
import scala.collection.immutable.SortedSet
  private def assertNonEmptyFetch(log: UnifiedLog, offset: Long, isolation: FetchIsolation, batchBaseOffset: Long): Unit = {
    assertEquals(batchBaseOffset, readInfo.fetchOffsetMetadata.messageOffset)
  private def assertEmptyFetch(log: UnifiedLog, offset: Long, isolation: FetchIsolation, batchBaseOffset: Long): Unit = {
    assertEquals(batchBaseOffset, readInfo.fetchOffsetMetadata.messageOffset)
    val batchBaseOffsets = SortedSet[Long](0, 3, 5)
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17803: logsegment#read should return the base offset of the batch that contains startoffset rather than startoffse.

**Test**
Test modified in PartitionTest.scala covering the corrected behaviour.
