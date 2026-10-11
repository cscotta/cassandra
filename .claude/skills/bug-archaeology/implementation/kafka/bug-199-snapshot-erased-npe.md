**Commit**: 53b2935c51 · **Ticket**: KAFKA-18461
**Subsystem**: server-common/src/main/java/org/apache/kafka/timeline/Snapshot.java
**Category**: Missing null / bounds check

**Root cause**: `Snapshot.erase()` sets the internal `map` field to `null` to release memory after the snapshot is no longer needed. If `setDelta()`, `handleRevert()`, or `mergeFrom()` were called on an erased snapshot, they would immediately throw a `NullPointerException` with no context about why `map` was null. The fix adds `Objects.requireNonNull(map, "Snapshot cannot be accessed after erase is called.")` at the top of each of these methods, giving operators a clear diagnostic instead of a raw NPE.

**Before/After**:
```java
// BEFORE – raw NPE with no context
void setDelta(Revertable owner, Delta delta) {
    map.put(owner, delta);  // NPE if erased
}

// AFTER – explicit check with message
void setDelta(Revertable owner, Delta delta) {
    Objects.requireNonNull(map, "Snapshot cannot be accessed after erase is called.");
    map.put(owner, delta);
}
```

**How to spot**: When a field is set to `null` as part of a lifecycle transition (e.g., `close`, `erase`, `invalidate`), every method that dereferences that field is a potential NPE. Add explicit null-checks with descriptive messages to all such methods.

**Test**: No test added. The fix improves error diagnostics in existing code paths that are exercised by `SnapshotRegistryTest` and `TimelineHashMapTest`.
