# Bug 0794: Gossip Blocked During Pending Range Calculation

**Commit**: 9cd7d540de · **Ticket**: CASSANDRA-12281
**Subsystem**: Gossip / Token metadata
**Category**: Deadlock

---

## Root Cause
`TokenMetadata.calculatePendingRanges()` held the global read lock for the entire duration of the calculation, which can be expensive (it calls `strategy.calculateNaturalEndpoints()` for every affected range and every bootstrapping/moving node). Because gossip frequently acquires the same read lock, a long-running pending-range computation could block gossip heartbeats and state updates for its entire duration, potentially causing false failure detection or delayed topology convergence.

The fix takes a snapshot of the mutable token-metadata fields under the read lock, releases the lock, and then performs the heavy computation on the snapshot, holding only the lightweight `pendingRanges` monitor to prevent concurrent writers from publishing a stale result.

### Before / After
```java
// BEFORE — holds read lock across all range calculations
lock.readLock().lock();
try {
    // ... expensive calculateNaturalEndpoints loops ...
    pendingRanges.put(keyspaceName, newPendingRanges);
} finally {
    lock.readLock().unlock();
}

// AFTER — snapshot under read lock, compute outside it
synchronized (pendingRanges) {
    lock.readLock().lock();
    try {
        bootstrapTokens.putAll(this.bootstrapTokens);   // snapshot
        leavingEndpoints.addAll(this.leavingEndpoints);
        metadata = this.cloneOnlyTokenMap();
    } finally {
        lock.readLock().unlock();                        // released before heavy work
    }
    pendingRanges.put(keyspaceName,
        calculatePendingRanges(strategy, metadata, ...)); // pure computation, no lock
}
```

---

## How to Spot
Look for read-lock critical sections that perform unbounded or O(N*M) work such as nested loops over tokens, replicas, or ranges. These are good candidates for a "snapshot-and-release" refactor: take a minimal snapshot while holding the lock, then do the expensive work outside it. The key invariant to preserve is that the published result is consistent with a single coherent snapshot.

---

## Test
`PendingRangeCalculatorServiceTest` was added, verifying that pending ranges are calculated correctly without requiring the read lock to be held during the full computation.
