# Bug 1971: Columns Incorrectly Surviving Deletion During Two-Phase Compaction

**Commit**: d38446a1bb · **Ticket**: CASSANDRA-5799
**Subsystem**: src/java/org/apache/cassandra/db/DeletionTime.java, src/java/org/apache/cassandra/db/compaction/LazilyCompactedRow.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`DeletionTime.isDeleted()` checked whether a column was marked for deletion before comparing its timestamp against the deletion time's `markedForDeleteAt`. This meant that a live column whose most-recent change timestamp was older than a row deletion time would not be considered deleted, because `isMarkedForDelete()` returned false. The fix replaces the two-part check with `column.mostRecentLiveChangeAt() <= markedForDeleteAt`, which correctly identifies any column whose latest live write falls before or at the row-level deletion time, regardless of whether the column itself carries a tombstone marker.

### Before / After

```java
// BEFORE - only considers a column deleted if it itself is tombstoned
public boolean isDeleted(IColumn column)
{
    return column.isMarkedForDelete() && column.getMarkedForDeleteAt() <= markedForDeleteAt;
}
```

```java
// AFTER - any column whose latest live change predates the row deletion is considered deleted
public boolean isDeleted(IColumn column)
{
    return column.mostRecentLiveChangeAt() <= markedForDeleteAt;
}
```

---

## How to Spot

When checking whether an entity falls within a deletion window, ensure the condition does not require the entity to itself carry a tombstone marker — a row-level or range deletion should supersede any live column whose write timestamp predates it, even if that column has no individual tombstone.

---

## Test

No test added.
