**Commit**: 16c079ed23 · **Ticket**: KAFKA-19525

**Subsystem**: Remote Storage / TopicBasedRemoteLogMetadataManager

**Category**: Race condition / missing synchronization

**Root cause**
`TopicBasedRLMM` used a plain `boolean configured` field guarded by coarse-grained locking, and a `startConsumerThread` method that could be invoked multiple times. Replacing the field with an `AtomicBoolean` and extracting the `withReadLockAndEnsureInitialized` helper ensures that the initialization check and the guarded operation are atomic from the caller's perspective, preventing a race where a second thread could slip past the not-yet-set flag and start a second consumer thread.

**Before/After**
```java
// Before: non-atomic initialized check
private boolean configured = false;

public void someOp() {
    if (!configured) throw new RemoteStorageException("not configured");
    // ... work ...
}

// After: atomic boolean with helper
private final AtomicBoolean configured = new AtomicBoolean(false);

private <T> T withReadLockAndEnsureInitialized(Supplier<T> action) {
    readLock.lock();
    try {
        if (!configured.get()) throw new RemoteStorageException("not configured");
        return action.get();
    } finally { readLock.unlock(); }
}
```

**How to spot**
In classes that guard initialization with a mutable boolean field shared across threads, look for check-then-act patterns where the boolean read and the subsequent action are not performed under the same lock. Converting to `AtomicBoolean` alone is insufficient; the check must be inside the critical section.

**Test**
`TopicBasedRemoteLogMetadataManagerTest` verifies that concurrent initialization and operation calls do not start duplicate consumer threads or access uninitialized state.
