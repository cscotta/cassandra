# Bug 0684: Race Condition in PendingRepairManager.getScanners Uses get() Instead of getOrCreate()

**Commit**: 6a7fad6011 · **Ticket**: CASSANDRA-13659
**Subsystem**: `src/java/org/apache/cassandra/db/compaction/PendingRepairManager.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

`PendingRepairManager.getScanners()` iterates a map of pending-repair session SSTables and calls `get(sessionId)` to look up the compaction strategy for each session. If the compaction strategy had not yet been created for a session that was already tracked in the SSTable map — which can happen when a repair session is added concurrently — `get()` returns null and a NullPointerException follows. The fix replaces `get()` with `getOrCreate()`, which lazily initialises the strategy if it is absent, matching the defensive pattern used elsewhere in the same class.

### Before / After

```java
// BEFORE
scanners.addAll(get(entry.getKey()).getScanners(entry.getValue(), ranges).scanners);
```

```java
// AFTER
scanners.addAll(getOrCreate(entry.getKey()).getScanners(entry.getValue(), ranges).scanners);
```

---

## How to Spot

In any class that maintains a map of lazily-initialised objects, ensure every public read path uses the same "get or create" helper as the write path; calls to the raw `get()` method are suspects when the map can be populated from multiple threads. The symptom is typically a NullPointerException at a map lookup rather than a data-race assertion failure, making it easy to mistake for a logic error.

---

## Test

No test added.
