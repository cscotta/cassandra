**Commit**: d6cd83e2fb · **Ticket**: KAFKA-16200
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Improve consistency and correctness for user-provided timeouts at the Consumer network request layer, per the Java client Consumer timeouts design (https://cwiki.apache.org/confluence/display/KAFKA/Java+client+Consumer+timeouts). While the changes introduced in KAFKA-15974 enforce timeouts at the Consumer's event layer, this change enforces timeouts at the network request layer. The changes mostly fit into the following areas:

1. Create shared code and idioms so timeout handling logic is consistent across current and future RequestManager implementations
2.

**Before/After**
```java
// Before
                Optional.empty());
    public CompletableFuture<Void> maybeAutoCommitSyncBeforeRevocation(final long retryExpirationTimeMs) {
            createOffsetCommitRequest(subscriptions.allConsumed(), Optional.of(retryExpirationTimeMs));
                    if (error instanceof TimeoutException && requestAttempt.isExpired) {
                        result.completeExceptionally(error);
        OffsetCommitRequestState commitRequest = createOffsetCommitRequest(offsets, Optional.empty());

// After
    private final Time time;
        this.time = time;
    private KafkaException maybeWrapAsTimeoutException(Throwable t) {
        if (t instanceof TimeoutException)
            return (TimeoutException) t;
        else
            return new TimeoutException(t);
    }

                Long.MAX_VALUE);
    public CompletableFuture<Void> maybeAutoCommitSyncBeforeRevocation(final long deadlineMs) {
            createOffsetCommitRequest(subscriptions.allConsumed(), deadlineMs);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16200: enforce that requestmanager implementations respect user-provided timeout (#16031).

**Test**
Test modified in CommitRequestManagerTest.java covering the corrected behaviour.
