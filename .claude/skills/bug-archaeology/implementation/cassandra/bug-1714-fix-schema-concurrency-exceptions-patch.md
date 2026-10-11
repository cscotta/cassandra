# Bug 1714: Fix Schema Concurrency Exceptions

**Commit**: bfd03f1939 · **Ticket**: CASSANDRA-6841
**Subsystem**: config/Schema, db/commitlog, utils/ConcurrentBiMap
**Category**: Race condition / missing synchronization

---

## Root Cause

This is the same fix as CASSANDRA-6841 applied to a different branch. `Schema.cfIdMap` was backed by a non-thread-safe `HashBiMap`, causing `ConcurrentModificationException` under concurrent schema access. In `CommitLogSegment`, `cfLastWrite` was accessed without synchronization and `getDirtyCFIDs()` returned a live mutable keyset, creating a hazard for any caller iterating while another thread modified the map. The fix introduces `ConcurrentBiMap` for the schema map and adds `synchronized` to all three `CommitLogSegment` methods that access `cfLastWrite`, returning a defensive copy from `getDirtyCFIDs()`.

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

When a shared mutable map field is declared as a non-concurrent type but methods that access it are not synchronized, concurrent access causes data races. Returning internal collection views (e.g., `keySet()`) from shared state compounds the hazard because callers receive a live view of the mutable map.

---

## Test

No test added.
