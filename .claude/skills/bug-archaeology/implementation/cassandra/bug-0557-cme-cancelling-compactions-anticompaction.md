# Bug 0557: ConcurrentModificationException When Cancelling Compactions for Anticompaction

**Commit**: 6b3ea1e1a4 · **Ticket**: CASSANDRA-15036
**Subsystem**: `src/java/org/apache/cassandra/db/compaction/ActiveCompactions.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

`ActiveCompactions.getCompactionForSSTable` iterated the `compactions` collection without holding its monitor. Because anticompaction can add or remove entries from `compactions` on another thread simultaneously (e.g., when beginning or ending a compaction holder), iterating without synchronization caused a `ConcurrentModificationException`. The fix wraps the iteration in a `synchronized (compactions)` block, matching the synchronization already used by the collection's add/remove paths.

### Before / After

```java
// BEFORE
for (CompactionInfo.Holder holder : compactions) {
    if (holder.getCompactionInfo().getSSTables().contains(sstable)) {
        ...
    }
}
```

```java
// AFTER
synchronized (compactions) {
    for (CompactionInfo.Holder holder : compactions) {
        if (holder.getCompactionInfo().getSSTables().contains(sstable)) {
            ...
        }
    }
}
```

---

## How to Spot

Whenever a shared collection is read (iterated) without synchronization, but written with synchronization elsewhere, the iteration is unsafe under concurrent mutation. Search for `for (... : sharedCollection)` patterns on fields that are also modified inside `synchronized` blocks — if the read path lacks the same guard it is a CME waiting to happen.

---

## Test

No test added.
