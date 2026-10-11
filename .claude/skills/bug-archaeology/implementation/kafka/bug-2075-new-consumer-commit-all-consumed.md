**Commit**: b213c64f97 · **Ticket**: KAFKA-17480
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, Kirk True <ktrue@confluent.io>, TengYao Chi <kitingiao@gmail.com>

**Before/After**
```java
// Before
    private CompletableFuture<Void> lastPendingAsyncCommit = null;
        commitAsync(subscriptions.allConsumed(), callback);
            lastPendingAsyncCommit = commit(asyncCommitEvent).whenComplete((r, t) -> {
                if (t == null) {
                    offsetCommitCallbackInvoker.enqueueInterceptorInvocation(offsets);
                    if (t != null) {
                        log.error("Offset commit with offsets {} failed", offsets, t);
                offsetCommitCallbackInvoker.enqueueUserCallbackInvocation(callback, offsets, (Exception) t);
    private CompletableFuture<Void> commit(final CommitEvent commitEvent) {
        Map<TopicPartition, OffsetAndMetadata> offsets = commitEvent.offsets();
        log.debug("Committing offsets: {}", offsets);
        offsets.forEach(this::updateLastSeenEpochIfNewer);

// After
    private CompletableFuture<Map<TopicPartition, OffsetAndMetadata>> lastPendingAsyncCommit = null;
        commitAsync(Optional.empty(), callback);
        commitAsync(Optional.of(offsets), callback);
    }

    private void commitAsync(Optional<Map<TopicPartition, OffsetAndMetadata>> offsets, OffsetCommitCallback callback) {
            lastPendingAsyncCommit = commit(asyncCommitEvent).whenComplete((committedOffsets, throwable) -> {
                if (throwable == null) {
                    offsetCommitCallbackInvoker.enqueueInterceptorInvocation(committedOffsets);
                    if (throwable != null) {
                        log.error("Offset commit with offsets {} failed", committedOffsets, throwable);
                offsetCommitCallbackInvoker.enqueueUserCallbackInvocation(callback, committedOffsets, (Exception) throwable);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17480: new consumer commit all consumed should retrieve offsets in background thread (#17150).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
