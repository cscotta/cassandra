**Commit**: 6f4ce76d84 · **Ticket**: KAFKA-19479
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java
**Category**: State not cleaned up

**Root cause**
When a batch was too large for the broker (`MESSAGE_TOO_LARGE`) it was split into smaller batches. The original batch's `ProduceRequestResult` was marked done immediately, but `RecordAccumulator.awaitFlush()` called `result.await()` — which only waited for the original result's latch and not for the split sub-batches. This meant `flush()` could return before all split batches had been sent, silently dropping records in `at_least_once` Kafka Streams pipelines. The fix introduces `awaitAllDependents()`, which traverses a linked list of dependent `ProduceRequestResult` objects registered at split time.

**Before/After**
```java
// Before: flush() only awaited the original batch's latch; split sub-batches ignored
for (ProduceRequestResult result : this.incomplete.requestResults())
    result.await();

// After: flush() awaits the original AND all split sub-batch results
for (ProduceRequestResult result : this.incomplete.requestResults())
    result.awaitAllDependents();
```

**How to spot**
Any pattern where a `CountDownLatch` or `CompletableFuture` representing a compound operation is completed before all its sub-operations finish can cause premature completion. In producer code, whenever a batch is split, all new batches should be tracked so that `flush()` and other barrier operations honour them. Search for `produceFuture.done()` calls in split paths and verify downstream awaiting logic covers sub-results.

**Test**
`RecordAccumulatorTest` adds `testSplitAndReenqueuePreventInfiniteRecursion` which creates oversized batches, triggers splitting, and verifies all 100 records survive the split process without loss or duplication.
