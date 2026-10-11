**Commit**: 388739f5d8 · **Ticket**: KAFKA-19760
**Subsystem**: coordinator-common/src
**Category**: Wrong serialization / deserialization

**Root cause**
The group coordinator has been having issues with unknown errors. The
theory is that this is caused by optimistic compression estimates which
cause unchecked batch overflows when trying to write. This PR adds a check for uncompressed record size to flush batches more
eagerly and avoid overfilling partially-full batches. This should make
the group coordinator errors less frequent.

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.RecordTooLargeException;
         * Flushes the current batch if it is transactional or if it has passed the append linger time.
                if (currentBatch.builder.isTransactional() || (currentBatch.appendTimeMs - currentTimeMs) >= appendLingerMs) {
                // the response can be returned directly iff there are no pending write operations;
                // otherwise, the read needs to wait on the last write operation to be completed.
                if (currentBatch != null && currentBatch.builder.numRecords() > 0) {
                    currentBatch.deferredEvents.add(event);
                } else {
                    if (coordinator.lastCommittedOffset() < coordinator.lastWrittenOffset()) {
                        deferredEventQueue.add(coordinator.lastWrittenOffset(), DeferredEventCollection.of(log, event));
                    } else {
                        event.complete(null);

// After
import org.apache.kafka.common.record.CompressionType;
         * Flushes the current batch if it is transactional, if it has passed the append linger time, or if it is full.
                if (currentBatch.builder.isTransactional() || (currentBatch.appendTimeMs - currentTimeMs) >= appendLingerMs || !currentBatch.builder.hasRoomFor(0)) {
        /**
         * Completes the given event once all pending writes are completed.
         *
         * @param event             The event to complete once all pending
         *                          writes are completed.
         */
        private void waitForPendingWrites(DeferredEvent event) {
            if (currentBatch != null && currentBatch.builder.numRecords() > 0) {
                currentBatch.deferredEvents.add(event);
```

**How to spot**
When reviewing coordinator-common/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19760: recordtoolargeexceptions in group coordinator when offsets.topic.compression.codec is used (#20653).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
