# Bug 1712: Fix Schema Concurrency Exceptions

**Commit**: 22b94537e0 · **Ticket**: CASSANDRA-6841
**Subsystem**: config/Schema, db/commitlog, utils/ConcurrentBiMap
**Category**: Race condition / missing synchronization

---

## Root Cause

`Schema.cfIdMap` was a non-thread-safe `HashBiMap` accessed concurrently from multiple threads, causing `ConcurrentModificationException` and other race-induced failures. In `CommitLogSegment`, the `cfLastWrite` map was also accessed without synchronization, and `getDirtyCFIDs()` returned a live keyset view that could be modified while callers iterated it. Additionally, `CommitLogAllocator.maybeFlushToReclaim()` would attempt to flush the currently active segment, which was never safe.

### Before / After

```java
// BEFORE
private final BiMap<Pair<String, String>, UUID> cfIdMap = HashBiMap.create();
```

```java
// AFTER
private final ConcurrentBiMap<Pair<String, String>, UUID> cfIdMap = new ConcurrentBiMap<>();
```

```java
// BEFORE
public Collection<UUID> getDirtyCFIDs() {
    return cfLastWrite.keySet();
}
```

```java
// AFTER
public synchronized Collection<UUID> getDirtyCFIDs() {
    return new ArrayList<>(cfLastWrite.keySet());
}
```

---

## How to Spot

When a shared mutable map field is declared as a non-concurrent type (e.g., `HashMap`, `HashBiMap`) but methods that read or write it are not synchronized, any concurrent access will cause data races. Pay special attention to methods that return internal collection views — callers may iterate while another thread mutates.

---

## Test

No test added.
