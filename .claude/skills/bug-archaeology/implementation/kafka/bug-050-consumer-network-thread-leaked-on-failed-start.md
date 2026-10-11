**Commit**: 48f6d7680e · **Ticket**: KAFKA-19898
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/events/ApplicationEventHandler.java
**Category**: Resource leak

**Root cause**
`ApplicationEventHandler`'s constructor started the `ConsumerNetworkThread` and assigned it to `this.networkThread` in two separate statements. If `networkThread.start(initializationTimeoutMs)` threw an exception (e.g., due to a timeout), `this.networkThread` had already been assigned, but the thread was still running and holding resources. The background thread was never closed, leaking it and eventually causing out-of-memory errors in unit tests that construct many consumers.

**Before/After**
```java
// Before – assignment before start; leaked if start throws
this.networkThread = new ConsumerNetworkThread(...);
this.networkThread.start(initializationTimeoutMs);

// After – assign only after successful start; close on failure
ConsumerNetworkThread networkThread = new ConsumerNetworkThread(...);
try {
    networkThread.start(initializationTimeoutMs);
} catch (Exception e) {
    try { networkThread.close(); } finally { networkThread = null; }
    throw e;
} finally {
    this.networkThread = networkThread;
}
```

**How to spot**
Any constructor that starts a background thread and then may throw should use a try/catch/finally to guarantee the thread is closed if initialization fails. The pattern "create → assign to field → start" should be "create → start → assign to field" or use a cleanup guard.

**Test**
No dedicated test added; the fix was prompted by OOM failures in existing client unit tests caused by leaked threads accumulating across test runs.
