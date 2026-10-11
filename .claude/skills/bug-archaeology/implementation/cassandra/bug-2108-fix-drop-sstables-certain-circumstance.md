# Bug 2108: SSTable Drop During Memtable Flush Replaces Live SSTables with Empty Set

**Commit**: 5632a56c46 · **Ticket**: CASSANDRA-5232
**Subsystem**: src/java/org/apache/cassandra/db/DataTracker.java
**Category**: State not cleaned up

---

## Root Cause

In `DataTracker`, when a memtable flush completes without producing a new SSTable (`newSSTable == null`), the updated `View` was constructed with `Collections.emptySet()` as the SSTable set. This discarded all previously-known SSTables for that column family, causing them to vanish from the tracker's view even though they were still on disk and valid. The correct behavior is to preserve the existing `sstables` set unchanged when no new file is produced.

### Before / After

```java
// BEFORE
Set<SSTableReader> newSSTables = newSSTable == null
    ? Collections.<SSTableReader>emptySet()
    : newSSTables(newSSTable);
```

```java
// AFTER
Set<SSTableReader> newSSTables = newSSTable == null
    ? sstables
    : newSSTables(newSSTable);
```

---

## How to Spot

Whenever constructing a replacement view/state from an optional value (null = "no change"), the null branch should preserve the existing state, not substitute an empty or default placeholder. Review every conditional that could substitute an empty collection for an existing one.

---

## Test

No test added.
