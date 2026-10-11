# Bug 2021: Unnecessary Second Collation Pass on Name-Based Queries

**Commit**: 94d42a9b0a · **Ticket**: CASSANDRA-5577
**Subsystem**: src/java/org/apache/cassandra/db/CollationController.java, src/java/org/apache/cassandra/db/ColumnFamily.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CollationController` always ran a final `filter.collateOnDiskAtom()` pass after merging SSTable data, even when no tombstones were present and the container was not marked for deletion. This was wasteful for the common case, but worse, the `addIfRelevant` logic in `ColumnFamily` incorrectly accepted columns whose timestamp predated a range tombstone (`column.timestamp() > column.getMarkedForDeleteAt()` was an unintended extra condition). Together, the logic could include columns that should have been shadowed by row or range tombstones. The fix skips the second pass when the container has no deletion info, and tightens `addIfRelevant` to check only GC eligibility.

### Before / After

```java
// BEFORE — always did second collation pass
ColumnFamily returnCF = container.cloneMeShallow();
Tracing.trace("Collating all results");
filter.collateOnDiskAtom(returnCF, container.iterator(), gcBefore);
```

```java
// AFTER — second pass only when container is marked for delete
if (container.isMarkedForDelete())
{
    returnCF = container.cloneMeShallow();
    Tracing.trace("Removing shadowed cells");
    filter.collateOnDiskAtom(returnCF, container.iterator(), gcBefore);
}
else
{
    returnCF = container;
}
```

```java
// BEFORE (ColumnFamily.addIfRelevant) — extra condition could admit shadowed columns
if ((column.getLocalDeletionTime() >= gcBefore || column.timestamp() > column.getMarkedForDeleteAt())
    && (!deletionInfo().isDeleted(column.name(), column.timestamp())))
```

```java
// AFTER — only GC eligibility checked
if ((column.getLocalDeletionTime() >= gcBefore)
    && (!deletionInfo().isDeleted(column.name(), column.timestamp())))
```

---

## How to Spot

Look for unconditional post-processing passes (collation, filtering, copying) that could be guarded by a cheaper predicate check. Also watch for compound boolean conditions in `addIfRelevant`-style guards where the disjunction unintentionally widens the set of accepted items.

---

## Test

No test added.
