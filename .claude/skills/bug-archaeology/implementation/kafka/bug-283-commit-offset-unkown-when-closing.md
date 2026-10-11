**Commit**: d84e19dde4 · **Ticket**: KAFKA-19712
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
KAFKA-19712 got rid of sentinel values when uninitialized offsets were
committed. This made state stores close with invalid offsets when the
store's offsets were not yet initialized. These changes rollback this decision because it was causing
TaskCorruptedException during rebalances. Reviewers: Bill Bejeck <bbejeck@gmail.com>

**Before/After**
```java
// Before
                    store.setOffset(offset);
                    if (metadata.changelogPartition == null || metadata.offset == null || metadata.corrupted || !store.persistent()) {
                        store.commit(Map.of(metadata.changelogPartition, metadata.offset));
import org.hamcrest.Matchers;
            assertThat(persistentStore.getLastCommitCount(), Matchers.lessThan(nonPersistentStore.getLastCommitCount()));

// After
import static org.apache.kafka.streams.state.internals.OffsetCheckpoint.OFFSET_UNKNOWN;
                    store.setOffset(changelogOffsetFromCommittedOffset(offset));
                    if (metadata.changelogPartition == null || metadata.corrupted || !store.persistent()) {
                        // logged store, persistent and valid end offset
                        store.commit(Map.of(metadata.changelogPartition, committableOffsetFromChangelogOffset(metadata.offset)));
    // Commit a sentinel value when the changelog offset is not yet initialized/known
    private long committableOffsetFromChangelogOffset(final Long offset) {
        return offset != null ? offset : OFFSET_UNKNOWN;
    }

    // Convert the written offsets in the checkpoint file back to the changelog offset
    private Long changelogOffsetFromCommittedOffset(final long offset) {
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19712: commit offset_unkown when closing state stores and the offsets are not yet initialized (#21884).

**Test**
Test modified in ProcessorStateManagerTest.java covering the corrected behaviour.
