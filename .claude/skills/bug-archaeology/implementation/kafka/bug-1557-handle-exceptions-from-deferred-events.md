**Commit**: 5a57473a52 · **Ticket**: KAFKA-18484
**Subsystem**: coordinator-common/src
**Category**: Resource leak

**Root cause**
Guard against the coordinator getting stuck due to deferred events
throwing exceptions. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
    private static class CoordinatorBatch {
         * The list of deferred events associated with the batch.
        final List<DeferredEvent> deferredEvents;
            this.deferredEvents = new ArrayList<>();
                    for (DeferredEvent event : currentBatch.deferredEvents) {
                        deferredEventQueue.add(offset, event);
                    }
                for (DeferredEvent event : currentBatch.deferredEvents) {
                    event.complete(t);
                }

// After
    private class CoordinatorBatch {
         * The deferred events associated with the batch.
        final DeferredEventCollection deferredEvents;
            this.deferredEvents = new DeferredEventCollection();
                    deferredEventQueue.add(offset, currentBatch.deferredEvents);
                currentBatch.deferredEvents.complete(t);
    /**
     * A collection of {@link DeferredEvent}. When completed, completes all the events in the collection
     * and logs any exceptions thrown.
     */
    class DeferredEventCollection implements DeferredEvent {
        private final List<DeferredEvent> events = new ArrayList<>();
```

**How to spot**
When reviewing coordinator-common/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18484 [1/n]; handle exceptions from deferred events in coordinator (#18661).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
