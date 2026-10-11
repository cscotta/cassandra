# Bug 1834: Fix Thundering Herd on Endpoint Cache Invalidation

**Commit**: 8145c83566 · **Ticket**: CASSANDRA-6345
**Subsystem**: src/java/org/apache/cassandra/locator
**Category**: Race condition / missing synchronization

---

## Root Cause

`TokenMetadata` previously maintained a list of `AbstractReplicationStrategy` subscribers and called `invalidateCachedTokenEndpointValues()` on each when the ring changed. Every strategy would then clear its endpoint cache, and the first concurrent `getNaturalEndpoints()` call would re-clone the full `TokenMetadata` and recompute all endpoint assignments. Under heavy traffic, many threads would simultaneously discover the empty cache, each clone the token map, and each recompute — a classic thundering herd. The fix replaces the push-notification/subscriber model with a version-counter approach: `TokenMetadata` merely increments a `ringVersion` long; each `AbstractReplicationStrategy` keeps its own cached `TokenMetadata` clone plus the version at which it was taken. On a cache hit the version is checked cheaply; on a miss, a `synchronized` double-checked locking block ensures only one thread rebuilds the clone.

### Before / After

```java
// BEFORE — every ring change notified all strategies and flushed their caches immediately
public void invalidateCaches() {
    for (AbstractReplicationStrategy subscriber : subscribers)
        subscriber.invalidateCachedTokenEndpointValues();
}
// then in getNaturalEndpoints(), each thread cloned TM independently:
TokenMetadata tokenMetadataClone = tokenMetadata.cloneOnlyTokenMap();
```

```java
// AFTER — version-counter + double-checked locking in AbstractReplicationStrategy
private volatile TokenMetadata tokenMetadataClone = null;
private volatile long clonedTokenMetadataVersion = 0;

// One thread rebuilds the clone; others wait and reuse it
if (tokenMetadataClone == null) {
    synchronized (this) {
        if (tokenMetadataClone == null)
            tokenMetadataClone = tokenMetadata.cloneOnlyTokenMap();
    }
}
```

---

## How to Spot

Whenever a cache invalidation is broadcast to multiple holders, check whether all holders will race to repopulate concurrently. If repopulation is expensive (e.g. cloning a large data structure), a single-writer pattern (double-checked locking or a `Semaphore`) should guard the rebuild.

---

## Test

No test added.
