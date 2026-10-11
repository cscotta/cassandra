# Bug 0473: LegacyLayout Non-Selected Collection Tombstone Stops Row Grouping

**Commit**: c8a2834606 · **Ticket**: CASSANDRA-15805
**Subsystem**: `src/java/org/apache/cassandra/db/LegacyLayout.java`
**Category**: Logic error in condition

---

## Root Cause

`LegacyLayout.CellGrouper#addCollectionTombstone` returned `false` when a collection tombstone's column was not selected by the query's `ColumnFilter`. Returning `false` from `addAtom` signals the caller that the current row is finished and a new row begins, but a non-selected collection tombstone within the same clustering is still part of the current row and should not terminate row grouping. The fix changes the return value to `true` so that grouping continues regardless of whether the collection is fetched.

### Before / After

```java
// BEFORE
if (!helper.includes(tombstone.start.collectionName))
    return false; // see CASSANDRA-13109
```

```java
// AFTER
// We can skip it (so return), but we're still within the current row
if (!helper.includes(tombstone.start.collectionName))
    return true; // see CASSANDRA-13109
```

---

## How to Spot

Whenever a boolean-returning method signals "continue" vs "stop" using `true`/`false`, review every early-return path carefully to confirm which signal it sends. Here, an early-return that was meant to say "nothing to do, keep going" was sending the wrong signal ("row is done"). Any `addAtom`-style accumulator that uses the return value as a termination sentinel is susceptible to this class of confusion when new skip/ignore branches are added.

---

## Test

`testCellGrouperOnNonFecthedCollectionTombstone` in `LegacyLayoutTest` adds a cell for column `a`, a non-fetched collection tombstone for `b`, and a cell for `c` to the same clustering, asserting all three `addAtom` calls return `true`.
