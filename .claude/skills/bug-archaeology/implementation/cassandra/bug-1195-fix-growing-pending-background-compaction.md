# Bug 1195: Background Compaction Startup Loop Submits Redundant Tasks, Inflating Pending Count

**Commit**: f283ed2981 · **Ticket**: CASSANDRA-9662
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: State not cleaned up

---

## Root Cause

On startup, `ColumnFamilyStore` submitted background compaction tasks in a loop: it called `submitBackground(cfs, false)` once per CFS, collected those that returned non-empty future lists into a `submitted` set, and then continued re-submitting those same stores in a second loop until the executor appeared to have free slots. Each iteration of the inner loop added another entry to `compactingCF` without a corresponding removal, so the `pendingTasks` metric — which counted queued entries in `compactingCF` — grew unboundedly. Additionally, the `autoFill` overload of `submitBackground` submitted multiple tasks per call based on available thread capacity, compounding the inflation. The fix removes the multi-submission loop entirely and submits each CFS exactly once, relying on the executor's own scheduling.

### Before / After

```java
// BEFORE — re-submits same stores multiple times, bloating compactingCF
List<ColumnFamilyStore> submitted = new ArrayList<>();
for (ColumnFamilyStore cfs : keyspace.getColumnFamilyStores())
    if (!CompactionManager.instance.submitBackground(cfs, false).isEmpty())
        submitted.add(cfs);
while (!submitted.isEmpty() && ...) {
    for (ColumnFamilyStore cfs : submitMore)
        if (!CompactionManager.instance.submitBackground(cfs, false).isEmpty())
            submitted.add(cfs);
}
```

```java
// AFTER — single submission per CFS
for (ColumnFamilyStore cfs : keyspace.getColumnFamilyStores())
    CompactionManager.instance.submitBackground(cfs);
```

---

## How to Spot

Any background-task submission that accumulates state in a shared collection (like `compactingCF`) without a matching cleanup, combined with a retry/fill loop, is at risk of unbounded growth. When reviewing metrics that report pending work, trace every write site of the underlying counter or collection and confirm each add is balanced by a remove.

---

## Test

`CompactionsTest` was updated to use a `do-while` loop waiting on `getPendingTasks()` and `getActiveCompactions()`, ensuring the fixed metric correctly reaches zero after compaction finishes.
