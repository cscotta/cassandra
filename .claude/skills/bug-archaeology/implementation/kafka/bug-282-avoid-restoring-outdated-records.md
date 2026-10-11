**Commit**: f55073b570 · **Ticket**: KAFKA-13499
**Subsystem**: streams/src
**Category**: Missing null / bounds check

**Root cause**
1. Expose the retentionPeriod length to storeMetadata
2. In prepareChangelogs(), switch it from always seektobeginning if
checkpoint doesn't exist to seek to certain timestamp to avoid restoring
outdated records. Reviewers: TengYao Chi <frankvicky@apache.org> , Bill Bejeck
 <bbejeck@apache.org>

**Before/After**
```java
// Before
                log.debug("Start restoring changelog partition {} from the beginning offset to end offset {} " +
                    "since we cannot find current offset.", partition, recordEndOffset(endOffset));

                newPartitionsWithoutStartOffset.add(partition);

// After
import org.apache.kafka.streams.state.internals.WithRetentionPeriod;
import org.apache.kafka.streams.state.internals.WrappedStateStore;
        private final long retentionPeriod;

            this.retentionPeriod = -1L;
            this.retentionPeriod = extractRetentionPeriod(stateStore);
        private static long extractRetentionPeriod(final StateStore stateStore) {
            StateStore current = stateStore;
            while (current instanceof WrappedStateStore) {
                current = ((WrappedStateStore<?, ?, ?>) current).wrapped();
            }
            if (current instanceof WithRetentionPeriod) {
```

**How to spot**
When reviewing streams/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13499: avoid restoring outdated records (#21901).

**Test**
Test modified in StoreChangelogReaderTest.java covering the corrected behaviour.
