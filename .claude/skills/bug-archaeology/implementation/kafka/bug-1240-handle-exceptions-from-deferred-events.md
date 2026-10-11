**Commit**: d7fa406b32 · **Ticket**: KAFKA-19064
**Subsystem**: coordinator-common/src
**Category**: Resource leak

**Root cause**
Previously in KAFKA-18484, we added exception handling for exceptions
coming from batch events. Also handle exceptions from 0-record write
events and transaction completion events. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
                        deferredEventQueue.add(coordinator.lastWrittenOffset(), event);
                deferredEventQueue.add(offset, event);
        // Get the max batch size.
        int maxBatchSize = writer.config(TP).maxMessageSize();

        // Create records with three quarters of the max batch size each, so that it is not
        // possible to have more than one record in a single batch.
        List<String> records = Stream.of('1', '2', '3').map(c -> {
            char[] payload = new char[maxBatchSize * 3 / 4];
            Arrays.fill(payload, c);
            return new String(payload);
        }).collect(Collectors.toList());

// After
                        deferredEventQueue.add(coordinator.lastWrittenOffset(), DeferredEventCollection.of(log, event));
                deferredEventQueue.add(offset, DeferredEventCollection.of(log, event));

        public static DeferredEventCollection of(Logger log, DeferredEvent... deferredEvents) {
            DeferredEventCollection collection = new DeferredEventCollection(log);
            for (DeferredEvent deferredEvent : deferredEvents) {
                collection.add(deferredEvent);
            }
            return collection;
        }
            state -> new CoordinatorResult<>(List.of("record1"), "response1")
        // Complete transaction #1, to force the flush of write #1.
```

**How to spot**
When reviewing coordinator-common/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19064: handle exceptions from deferred events in coordinator (#19333).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
