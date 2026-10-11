# Bug 1578: Fix CC#collectTimeOrderedData() Tombstone Optimisations

**Commit**: 77bbcc18fa · **Ticket**: CASSANDRA-7394
**Subsystem**: src/java/org/apache/cassandra/db/CollationController.java
**Category**: Logic error in condition

---

## Root Cause

`collectTimeOrderedData` initialised `mostRecentRowTombstone` only inside the SSTable loop, but the variable needed to reflect tombstones accumulated from memtables first. The SSTable early-exit optimisation (skipping SSTables whose max timestamp is older than the most recent row tombstone) therefore never considered tombstones that came from memtables. Additionally, the SSTable loop only advanced `mostRecentRowTombstone` when `cf.isMarkedForDelete()` was true, missing the case where a row tombstone arrived via the merge of multiple SSTables into the container. The fix moves the variable declaration before the memtable loop and updates it from the merged container after each source (memtable or SSTable) is merged in.

### Before / After

```java
// BEFORE – variable declared inside SSTable loop; memtable tombstones ignored
long mostRecentRowTombstone = Long.MIN_VALUE;   // declared here, inside SSTable loop
for (SSTableReader sstable : view.sstables)
{
    ...
    if (cf.isMarkedForDelete())
        mostRecentRowTombstone = cf.deletionInfo()...markedForDeleteAt;
    ...
    container.addAll(temp, HeapAllocator.instance);
    temp.clear();
}
```

```java
// AFTER – variable declared before memtable loop; updated from merged container
long mostRecentRowTombstone = Long.MIN_VALUE;   // declared here, before memtable loop
for (Memtable memtable : view.memtables)
{
    ...
    container.addAll(temp, HeapAllocator.instance);
    mostRecentRowTombstone = container.deletionInfo()...markedForDeleteAt;
    temp.clear();
}
...
for (SSTableReader sstable : view.sstables)
{
    ...
    container.addAll(temp, HeapAllocator.instance);
    mostRecentRowTombstone = container.deletionInfo()...markedForDeleteAt;
    temp.clear();
}
```

---

## How to Spot

When an optimisation variable is updated inside a conditional block (`if (cf.isMarkedForDelete())`), check whether the condition can be false while the underlying state has changed — especially after a merge operation. Prefer deriving the tracking variable from the already-merged result rather than from individual inputs.

---

## Test

No test added.
