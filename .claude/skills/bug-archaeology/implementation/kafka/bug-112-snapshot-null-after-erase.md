**Commit**: 53b2935c51 · **Ticket**: KAFKA-18461
**Subsystem**: server-common / timeline / Snapshot
**Category**: Missing null / bounds check

**Root cause**
`Snapshot.erase()` sets the internal `map` field to `null` to signal that the snapshot has been discarded. The three methods `setDelta`, `handleRevert`, and `mergeFrom` then dereference `map` unconditionally. If any caller retains a reference to a `Snapshot` after `erase()` is called (possible when the snapshot is being iterated while another thread erases it), a `NullPointerException` is thrown with a non-descriptive message. The fix adds `Objects.requireNonNull(map, "Snapshot cannot be accessed after erase is called.")` at the entry of each method.

**Before/After**
```java
// Before — silent NPE if map is null
void setDelta(Revertable owner, Delta delta) {
    map.put(owner, delta);
}

// After — explicit, descriptive null guard
void setDelta(Revertable owner, Delta delta) {
    Objects.requireNonNull(map, "Snapshot cannot be accessed after erase is called.");
    map.put(owner, delta);
}
```

**How to spot**
When an object's internal state is nulled out as part of a lifecycle method (close, erase, destroy), every public method that accesses that state should perform an explicit null check with an informative message. An IDE inspection for "field used after it can be set to null in a lifecycle method" would catch this pattern.

**Test**
No new test added; the commit improves the error message quality rather than adding new behavior tests.
