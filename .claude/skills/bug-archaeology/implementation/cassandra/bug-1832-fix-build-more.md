# Bug 1832: Fix Build — Make invalidateCachedRings() Public After Refactor

**Commit**: 5f62610969 · **Ticket**: CASSANDRA-6345
**Subsystem**: src/java/org/apache/cassandra/locator
**Category**: Wrong constant / default value

---

## Root Cause

The preceding thundering-herd refactor (commit 8145c83566) changed the cache-invalidation model in `TokenMetadata`: the old `invalidateCaches()` method (which iterated subscribers) was replaced by `invalidateCachedRings()` which just bumps a version counter. However, `PropertyFileSnitch` still called the old method name (`invalidateCaches()`), and the new `invalidateCachedRings()` was left `private`, preventing external callers from accessing it. Both problems broke compilation.

### Before / After

```java
// BEFORE (PropertyFileSnitch.java)
StorageService.instance.getTokenMetadata().invalidateCaches();

// BEFORE (TokenMetadata.java)
private void invalidateCachedRings() { ringVersion++; }
```

```java
// AFTER (PropertyFileSnitch.java)
StorageService.instance.getTokenMetadata().invalidateCachedRings();

// AFTER (TokenMetadata.java)
public void invalidateCachedRings() { ringVersion++; }
```

---

## How to Spot

When renaming or restructuring a method, search all callers before marking the old method for removal — especially when the method is referenced from unrelated classes (snitch, storage service) that lie outside the immediate change scope.

---

## Test

No test added.
