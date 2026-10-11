**Commit**: 3835515fea · **Ticket**: KAFKA-16541
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
A patch for KAFKA-15046 got rid of fsync on LeaderEpochFileCache#truncateFromStart/End for performance reason, but it turned out this could cause corrupted leader-epoch checkpoint file on ungraceful OS shutdown, i.e. OS shuts down in the middle when kernel is writing dirty pages back to the device. To address this problem, this PR makes below changes: (1) Revert LeaderEpochCheckpoint#write to always fsync
(2) truncateFromStart/End now call LeaderEpochCheckpoint#write asynchronously on scheduler thread
(3) UnifiedLog#maybeCreateLeaderEpochCache now loads epoch entries from checkpoint file only when current cache is absent

Reviewers: Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.storage.internals.checkpoint.InMemoryLeaderEpochCheckpoint;
     * Returns the leader epoch checkpoint by truncating with the given start[exclusive] and end[inclusive] offset
     * @param startOffset The start offset of the checkpoint file (exclusive in the truncation).
     * @param endOffset   The end offset of the checkpoint file (inclusive in the truncation)
     * @return the truncated leader epoch checkpoint
    InMemoryLeaderEpochCheckpoint getLeaderEpochCheckpoint(UnifiedLog log, long startOffset, long endOffset) {
        InMemoryLeaderEpochCheckpoint checkpoint = new InMemoryLeaderEpochCheckpoint();
            LeaderEpochFileCache cache = log.leaderEpochCache().get().writeTo(checkpoint);
            if (startOffset >= 0) {
                cache.truncateFromStart(startOffset);
            }
            cache.truncateFromEnd(endOffset);

// After
import org.apache.kafka.server.common.CheckpointFile;
import org.apache.kafka.storage.internals.checkpoint.LeaderEpochCheckpointFile;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
     * Returns the leader epoch entries within the range of the given start[exclusive] and end[inclusive] offset.
     * <p>
     * Visible for testing.
     * @param startOffset The start offset of the epoch entries (inclusive).
     * @param endOffset   The end offset of the epoch entries (exclusive)
     * @return the leader epoch entries
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16541 fix potential leader-epoch checkpoint file corruption (#15993).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
