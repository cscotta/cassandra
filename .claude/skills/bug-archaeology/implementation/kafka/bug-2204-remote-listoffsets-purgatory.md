**Commit**: 604564cce3 · **Ticket**: KAFKA-15859
**Subsystem**: core/replication (DelayedRemoteListOffsets, ReplicaManager)
**Category**: Logic error in condition

**Root cause**
`DelayedRemoteListOffsets` requests were never completed when the partition replica moved to another broker during the async fetch. The `tryComplete()` method checked partition state but did not cancel the outstanding remote storage futures when the partition was no longer local, causing requests to hang until expiry. Additionally, `completeDelayedFetchOrProduceRequests` did not include the remote list-offsets purgatory, so broker events that should have triggered completion were ignored.

**Before/After**
```scala
// Before – purgatory never triggered on replica change
def completeDelayedFetchOrProduceRequests(topicPartition: TopicPartition): Unit = {
  delayedProducePurgatory.checkAndComplete(key)
  delayedFetchPurgatory.checkAndComplete(key)
}

// After – remote list-offsets included; futures cancelled on ApiException
def completeDelayedFetchOrProduceRequests(tp: TopicPartition): Unit = {
  delayedProducePurgatory.checkAndComplete(key)
  delayedFetchPurgatory.checkAndComplete(key)
  delayedRemoteListOffsetsPurgatory.checkAndComplete(key)
}
```

**How to spot**
Whenever a new purgatory is introduced, audit every call site of `completeDelayed*` to confirm the new purgatory is checked. Missing entries mean delayed operations in the new purgatory can only complete via timeout, not via the fast path.

**Test**
Unit tests added in `DelayedRemoteListOffsetsTest` covering replica-move cancellation.
