**Commit**: b954b35d0a · **Ticket**: KAFKA-20332
**Subsystem**: clients — AsyncKafkaConsumer / AbstractMembershipManager
**Category**: Race condition / missing synchronization

**Root cause**
Two separate races allowed the application thread to return records for partitions that were being revoked. First, `collectFetch()` was called before the background thread had a chance to check for pending reconciliations, meaning revoked partitions had not yet been marked as non-fetchable. Second, `AbstractMembershipManager.maybeReconcile()` allowed the background thread to complete a reconciliation that included revocations while the app thread might already have those partitions' records buffered in memory. The fix adds a reconciliation-check synchronization point: the app thread waits on a `CompletableFuture` from the `AsyncPollEvent` before collecting from the buffer, and background reconciliations that involve revocations are deferred until the app-thread poll event runs.

**Before/After**
```java
// Before — collectFetch called unconditionally
private Fetch<K, V> pollForFetches(Timer timer) {
    final Fetch<K, V> fetch = collectFetch();   // may return revoked-partition records
    ...
}

// After — wait for reconciliation check before collecting
private Fetch<K, V> collectFetch() {
    if (inflightPoll != null && !inflightPoll.isReconciliationCheckComplete()) {
        // wait for background to check pending reconciliations
        ConsumerUtils.getResult(inflightPoll.reconciliationCheckFuture(), timeoutMs);
    }
    ...
}
```

**How to spot**
In a two-thread consumer model, any code path that reads from the fetch buffer before confirming the membership-manager's view of fetchable partitions is consistent is a candidate. Look for calls to `collectFetch()` / `fetchBuffer.nextInLineFetch()` that are not gated by a reconciliation-check synchronization point.

**Test**
`AbstractMembershipManagerTest` and `AsyncKafkaConsumerTest` — multiple new tests covering: background reconciliation skipped when revocations are pending without app-thread confirmation; app thread waits for reconciliation-check future before returning buffered records; `ShareMembershipManager` overrides `poll()` to always allow full reconciliation from background.
