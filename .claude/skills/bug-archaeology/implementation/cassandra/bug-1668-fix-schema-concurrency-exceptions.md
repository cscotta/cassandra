# Bug 1668: Fix Schema Concurrency Exceptions

**Commit**: 0547d16d5f · **Ticket**: CASSANDRA-6841
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/ (CommitLogSegment, CommitLogAllocator)
**Category**: Race condition / missing synchronization

---

## Root Cause

`CommitLogSegment` used an unsynchronized `HashMap<UUID, Integer>` (`cfLastWrite`) to track which column families had dirty writes in a segment. Three methods — `markClean`, `getDirtyCFIDs`, and `isUnused` — all accessed this map without holding a lock. Concurrent calls from the segment allocator thread and flusher threads could observe a partially-updated map, causing `ConcurrentModificationException` or incorrect "is dirty" decisions. Additionally, `CommitLogAllocator` would attempt forced flushes on the currently-active segment, which was unnecessary and could race with ongoing writes.

### Before / After

```java
// BEFORE – unsynchronized map access
public void markClean(UUID cfId, ReplayPosition context) { ... }
public Collection<UUID> getDirtyCFIDs() {
    return cfLastWrite.keySet(); // live view, unsafe to iterate externally
}
public boolean isUnused() { return cfLastWrite.isEmpty(); }
```

```java
// AFTER – all map accesses synchronized; snapshot returned from getDirtyCFIDs
public synchronized void markClean(UUID cfId, ReplayPosition context) { ... }
public synchronized Collection<UUID> getDirtyCFIDs() {
    return new ArrayList<UUID>(cfLastWrite.keySet()); // defensive copy
}
public synchronized boolean isUnused() { return cfLastWrite.isEmpty(); }
```

---

## How to Spot

Any shared mutable collection that is read and written from multiple threads without synchronization is suspect. Returning a live `keySet()` view from a method that callers will iterate is especially dangerous — always return a defensive copy or use a concurrent collection.

---

## Test

No test added.
