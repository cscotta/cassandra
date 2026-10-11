**Commit**: 402cb65ef1 · **Ticket**: KAFKA-13499
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This reverts commit f55073b570ff80d8e3eaeabce48ec361b3cbf18f.

**Before/After**
```java
// Before
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

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: revert "kafka-13499: avoid restoring outdated records (#21901)".

**Test**
Test modified in StoreChangelogReaderTest.java covering the corrected behaviour.
