# Bug 1344: Fix IllegalArgumentException in dynamic snitch

**Commit**: cf73de2dc9 · **Ticket**: CASSANDRA-8448
**Subsystem**: src/java/org/apache/cassandra/locator/DynamicEndpointSnitch.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`DynamicEndpointSnitch` maintained a `ConcurrentHashMap<InetAddress, Double>` called `scores` that was updated in-place by a background task while other threads were iterating over it to rank endpoints. Although `ConcurrentHashMap` allows safe concurrent puts, the comparator used during endpoint sorting computed relative latency ratios by reading stale intermediate values during an in-progress update cycle, which could produce inconsistent comparison results and trigger an `IllegalArgumentException` from the sort. The fix replaces the in-place mutation pattern with a copy-on-write approach: the background task builds a fresh `HashMap` and then atomically swaps the reference via a `volatile` field, so readers always see a fully consistent snapshot.

### Before / After

```java
// BEFORE
private final ConcurrentHashMap<InetAddress, Double> scores = new ConcurrentHashMap<>();
// ...
scores.put(entry.getKey(), score);   // updated in-place while readers iterate
```

```java
// AFTER
private volatile HashMap<InetAddress, Double> scores = new HashMap<>();
// ...
HashMap<InetAddress, Double> newScores = new HashMap<>();
// ... populate newScores ...
newScores.put(entry.getKey(), score);
scores = newScores;   // atomic reference swap
```

---

## How to Spot

When a shared collection is read by comparators or ranking logic at the same time a background maintenance task writes to it, look for the possibility that mid-update reads produce an internally inconsistent view. Even thread-safe collections like `ConcurrentHashMap` do not guarantee that a set of logically related reads forms an atomic snapshot; if the comparator contract (transitivity, consistency) depends on all values being from the same computation epoch, concurrent writes can violate it. A copy-on-write swap via a `volatile` reference is the standard remedy.

---

## Test

No test added.
