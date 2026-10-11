# Bug 1968: Range Tombstone Off-by-One and Lazy Tester Initialization

**Commit**: b4bcdfc154 · **Ticket**: CASSANDRA-5805
**Subsystem**: src/java/org/apache/cassandra/db/DeletionInfo.java, src/java/org/apache/cassandra/db/RangeTombstoneList.java
**Category**: Off-by-one

---

## Root Cause

Two separate bugs existed in the range tombstone machinery. First, `RangeTombstoneList.InOrderTester.isDeleted()` used a strict less-than comparison (`< 0`) when checking if a column name falls within a tombstone's end bound, causing columns exactly at the end boundary to escape deletion. Second, `DeletionInfo.InOrderTester` eagerly captured the `RangeTombstoneList` tester at construction time, but range tombstones can be added to a `DeletionInfo` incrementally while columns are being iterated; any tombstone added after the tester was created would be invisible to it.

### Before / After

```java
// BEFORE (RangeTombstoneList) - strict less-than misses end-inclusive boundary
if (comparator.compare(name, ends[idx]) < 0)
    return markedAts[idx] >= timestamp;

// BEFORE (DeletionInfo.InOrderTester) - tester created eagerly at construction, missing later-added ranges
private final RangeTombstoneList.InOrderTester tester;

private InOrderTester(boolean reversed)
{
    this.reversed = reversed;
    this.tester = ranges == null || reversed ? null : ranges.inOrderTester();
}
```

```java
// AFTER (RangeTombstoneList) - less-than-or-equal correctly includes end boundary
if (comparator.compare(name, ends[idx]) <= 0)
    return markedAts[idx] >= timestamp;

// AFTER (DeletionInfo.InOrderTester) - tester created lazily, picks up newly added ranges
private RangeTombstoneList.InOrderTester tester;  // non-final

public boolean isDeleted(Column column)
{
    // ...
    if (tester == null && ranges != null)
        tester = ranges.inOrderTester();
    return tester != null && tester.isDeleted(name, timestamp);
}
```

---

## How to Spot

When a range tombstone boundary is used in a comparison loop, check whether the boundary is inclusive or exclusive and ensure the operator matches the semantic contract. Also watch for tester/iterator objects captured from mutable collections at construction time when the collection may be populated lazily or incrementally.

---

## Test

No test added.
