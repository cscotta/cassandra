**Commit**: abcbef6a4c · **Ticket**: KAFKA-20131
**Subsystem**: clients — ClassicKafkaConsumer / OffsetFetcher / SubscriptionState
**Category**: State not cleaned up

**Root cause**
`ClassicKafkaConsumer.currentLag()` called `subscriptions.requestPartitionEndOffset(topicPartition)` to set the `endOffsetRequested` flag, then fired a `LIST_OFFSETS` request. If that request failed (network error, timeout), the flag was never cleared. On subsequent calls to `currentLag()`, the guard `!subscriptions.partitionEndOffsetRequested(topicPartition)` prevented any new `LIST_OFFSETS` request from being issued, so lag computation was permanently stalled for that partition until a rebalance or restart.

**Before/After**
```java
// Before — endOffsetRequested flag set but never cleared on failure
subscriptions.requestPartitionEndOffset(topicPartition);
offsetFetcher.endOffsets(Collections.singleton(topicPartition), time.timer(0L));
// if endOffsets() fails, flag stays set forever

// After — flag cleared on failure by OffsetFetcherUtils.clearPartitionEndOffsetRequests()
// called from ListOffsets onFailure callback (and on partial retry retry set)
```

**How to spot**
Any boolean "in-flight request" flag used for deduplication must be cleared on *both* success and failure paths. The success path was handled (the flag is implicitly cleared when the position is updated), but the failure path was not. Search for `requestPartitionEndOffset` / `endOffsetRequested` usages and verify every exit path (including exception/failure callbacks) resets the flag.

**Test**
The fix refactors `currentLag()` into `OffsetFetcher`, adds `maybeSetPartitionEndOffsetRequest()` and `clearPartitionEndOffsetRequests()` helpers, and threads an `updatePartitionEndOffsetsFlag` parameter through `fetchOffsetsByTimes`. Existing `OffsetFetcherTest` cases exercise the new flag-clearing logic.
