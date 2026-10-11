# Bug 0347: Token Cache Unnecessarily Invalidated When Removing a Non-Member Node

**Commit**: b1e77baa3d · **Ticket**: CASSANDRA-15290
**Subsystem**: src/java/org/apache/cassandra/locator/TokenMetadata.java
**Category**: Logic error in condition

---

## Root Cause

`TokenMetadata.removeEndpoint` unconditionally called `sortTokens()` and `invalidateCachedRingsUnsafe()` even when the endpoint being removed was not present in the `tokenToEndpointMap` (i.e. it had no tokens assigned, such as a proxy or bootstrap node that never completed joining). Invalidating the ring version counter for a no-op removal caused unnecessary cache misses and, more critically, incremented the ring version when nothing actually changed. Code that depends on ring-version stability to detect topology changes could react spuriously to these phantom invalidations.

### Before / After

```java
// BEFORE
tokenToEndpointMap.removeValue(endpoint);
// ... (always runs)
sortedTokens = sortTokens();
invalidateCachedRingsUnsafe();
```

```java
// AFTER
Collection<Token> removedTokens = tokenToEndpointMap.removeValue(endpoint);
if (removedTokens != null && !removedTokens.isEmpty())
{
    sortedTokens = sortTokens();
    invalidateCachedRingsUnsafe();
}
```

---

## How to Spot

Any side-effecting operation (cache invalidation, version bump, sorted-list rebuild) that is triggered by a removal should first check whether the removal actually changed state. Unconditional post-removal invalidation is correct but wasteful and can cause spurious downstream reactions when callers use the version number as a change signal.

---

## Test

Adds `testRemoveEndpointTokenChange` to `TokenMetadataTest`: asserts that removing a normal member increments the ring version, while removing a non-existent endpoint leaves the ring version unchanged.
