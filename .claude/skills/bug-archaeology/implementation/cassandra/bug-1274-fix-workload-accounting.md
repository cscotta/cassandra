# Bug 1274: Partition-Level-Delete-Only Workload Not Counted in Memtable Size

**Commit**: f0c7a6f03c · **Ticket**: CASSANDRA-9194
**Subsystem**: db/Memtable.java
**Category**: Off-by-one

---

## Root Cause

When a mutation creates a brand-new partition entry in the memtable (the `putIfAbsent` path succeeds), the row's key itself occupies heap space that must be reflected in `currentSize`. The code tracked the delta reported by `addAllWithSizeDelta` but omitted the +1 accounting unit for the newly allocated partition slot. For workloads consisting purely of partition-level deletes—where no column data is written and `addAllWithSizeDelta` returns a zero or near-zero delta—the size counter never grew, causing the memtable to appear perpetually near-empty and never triggering a flush. Over time this leads to unbounded memory growth.

### Before / After

```java
// BEFORE
if (previous == null)
    previous = rows.putIfAbsent(..., empty);
    if (previous == null)
        previous = empty;
// (no initialSize accounting)

final Pair<Long, Long> pair = previous.addAllWithSizeDelta(...);
currentSize.addAndGet(pair.left);
```

```java
// AFTER
long initialSize = 0;
if (previous == null)
    previous = rows.putIfAbsent(..., empty);
    if (previous == null)
    {
        previous = empty;
        initialSize += 1;
    }

final Pair<Long, Long> pair = previous.addAllWithSizeDelta(...);
currentSize.addAndGet(initialSize + pair.left);
```

---

## How to Spot

Whenever a data structure charges sizes based on deltas returned from a sub-component, check whether the act of *inserting* the new container itself contributes a fixed cost that the delta-reporting path cannot know about. The pattern `putIfAbsent` + `addAndGet(delta)` silently loses the container-creation overhead. Partition-delete-heavy workloads are the canary: if flushing does not occur even as heap grows, suspect that a fixed per-entry cost is missing from the size accounting.

---

## Test

No test added.
