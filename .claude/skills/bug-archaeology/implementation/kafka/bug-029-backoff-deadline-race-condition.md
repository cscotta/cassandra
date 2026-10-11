**Commit**: a739b05e51 · **Ticket**: KAFKA-20058
**Subsystem**: transaction-coordinator/src/main/java/org/apache/kafka/coordinator/transaction/RPCProducerIdManager.java
**Category**: Race condition / missing synchronization

**Root cause**
`RPCProducerIdManager.maybeRequestNextBlock()` sent the controller request asynchronously and then unconditionally reset `backoffDeadlineMs` to `NO_RETRY` in the request-sending thread. Concurrently, the response-handler thread could receive a failure and set `backoffDeadlineMs = now + RETRY_BACKOFF_MS`. Because the send-path reset ran after the response-path set, the valid backoff was clobbered, allowing an immediate re-send and causing flaky tests.

**Before/After**
```java
// Before: request path unconditionally resets backoff after send
if (nextProducerIdBlock.get() == null &&
        requestInFlight.compareAndSet(false, true)) {
    sendRequest();
    backoffDeadlineMs.set(NO_RETRY);  // races with response handler
}

// After: removed reset from request path; success/timeout paths reset instead
// In onTimeout():
backoffDeadlineMs.set(NO_RETRY);
requestInFlight.set(false);
// In handleAllocateProducerIdsResponse() on success:
backoffDeadlineMs.set(NO_RETRY);
```

**How to spot**
Any `AtomicLong` whose setter is called from two threads without a `compareAndSet` discipline is suspect. Here the invariant "only the response handler mutates `backoffDeadlineMs`" was not documented or enforced, so a later developer added the reset in the wrong place.

**Test**
`ProducerIdManagerTest#testRetryBackoffOnAuthException/testRetryBackoffOnVersionMismatch/testRetryBackoffOnNoResponse`: each injects a failure on the first send, asserts `CoordinatorLoadInProgressException` while the backoff is active, advances time by `RETRY_BACKOFF_MS`, then verifies a successful block is obtained on the next call.
