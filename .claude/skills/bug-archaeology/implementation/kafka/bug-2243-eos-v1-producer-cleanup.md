**Commit**: 8d186bfb4f · **Ticket**: KAFKA-16331
**Subsystem**: streams (ActiveTaskCreator, TaskManager, StreamThread)
**Category**: State not cleaned up

**Root cause**
Per-task producer support (from EOS-alpha / EOSv1) had been removed from Kafka Streams in terms of logic, but residual code — `streamsProducerForTask()`, `closeAndRemoveTaskProducerIfNeeded()`, and the `threadProducer` / `streamsProducer` naming inconsistency — remained in `ActiveTaskCreator`, `TaskManager`, and `TaskExecutor`. This dead code path obscured the actual EOS-v2 architecture, made the producer lifecycle harder to trace, and could confuse future contributors into thinking per-task producers were still supported.

**Before/After**
```java
// Before – per-task producer methods still present
StreamsProducer streamsProducerForTask(TaskId taskId) { ... }
void closeAndRemoveTaskProducerIfNeeded(TaskId taskId) { ... }
private StreamsProducer threadProducer;

// After – cleaned up, single producer per thread
// threadProducer renamed streamsProducer
// closeAndRemoveTaskProducerIfNeeded removed
// closeThreadProducerIfNeeded renamed close()
```

**How to spot**
After removing a feature (EOS-v1, per-task producers), search for residual helper methods whose names or signatures reference the removed feature; if they are unreachable from any non-deprecated code path, remove them.

**Test**
`StreamsProducerTest` and `RecordCollectorTest` updated; `MockClientSupplier` usage removed.
