**Commit**: 18f17ed4d3 · **Ticket**: KAFKA-18200
**Subsystem**: coordinator-common/src
**Category**: Resource leak

**Root cause**
* Avoid attaching empty writes to empty batches. * Handle flushes of empty batches, which would return a 0 offset otherwise. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
                if (currentBatch != null) {
@SuppressWarnings("checkstyle:JavaNCSS")

// After
                    if (currentBatch.builder.numRecords() == 0) {
                        // The only way we can get here is if append() has failed in an unexpected
                        // way and left an empty batch. Try to clean it up.
                        log.debug("Tried to flush an empty batch for {}.", tp);
                        // There should not be any deferred events attached to the batch. We fail
                        // the batch just in case. As a side effect, coordinator state is also
                        // reverted, but there should be no changes since the batch was empty.
                        failCurrentBatch(new IllegalStateException("Record batch was empty"));
                        return;
                    }

                if (currentBatch != null && currentBatch.builder.numRecords() > 0) {
```

**How to spot**
When reviewing coordinator-common/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18200; handle empty batches in coordinator runtime (#18144).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
