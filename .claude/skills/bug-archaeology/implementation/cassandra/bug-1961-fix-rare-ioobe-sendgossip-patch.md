# Bug 1961: Race condition in sendGossip causes IndexOutOfBoundsException

**Commit**: 2993965371 · **Ticket**: CASSANDRA-4774
**Subsystem**: src/java/org/apache/cassandra/gms/Gossiper.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`sendGossip` received a live endpoint `Set<InetAddress>`, read its `size()`, then immediately converted it to an `ArrayList` via `new ArrayList<>(epSet)`. Because the set is a shared, mutable collection, another thread could remove an endpoint between the size check and the list construction, making the list smaller than `size`. A random index computed from the original `size` could then exceed the shrunken list's bounds, causing an `IndexOutOfBoundsException`. The fix snapshots the set atomically into an `ImmutableList` before reading the size, so both values are consistent.

### Before / After

```java
// BEFORE
int size = epSet.size();
if (size < 1)
    return false;
List<InetAddress> liveEndpoints = new ArrayList<InetAddress>(epSet);
int index = (size == 1) ? 0 : random.nextInt(size);
InetAddress to = liveEndpoints.get(index);
```

```java
// AFTER
List<InetAddress> liveEndpoints = ImmutableList.copyOf(epSet);
int size = liveEndpoints.size();
if (size < 1)
    return false;
int index = (size == 1) ? 0 : random.nextInt(size);
InetAddress to = liveEndpoints.get(index);
```

---

## How to Spot

Any pattern that calls `collection.size()` and then constructs a snapshot from the same collection in two separate steps is subject to TOCTOU races when the collection is shared across threads. The snapshot must be taken first, and `size()` must be derived from the snapshot, not from the original collection.

---

## Test

No test added.
