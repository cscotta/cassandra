# Bug 0474: Legacy Range Tombstone Interacting with Collection Tombstone Causes Row Duplication

**Commit**: 8358e19840 · **Ticket**: CASSANDRA-15805
**Subsystem**: `src/java/org/apache/cassandra/db/LegacyLayout.java`, `src/java/org/apache/cassandra/db/UnfilteredDeserializer.java`
**Category**: Logic error in condition

---

## Root Cause

When a multi-row range tombstone coexists with a collection tombstone on one of its covered rows, the 2.x serialization format produces a range tombstone whose start bound falls inside that row (mid-clustering). The deserialization code adjusted this start bound to point to the end of the row rather than its beginning. `CellGrouper` uses the start bound position to decide whether a row is finished; receiving an end-of-row start caused it to terminate the current row prematurely, then process remaining cells as a new (duplicate) row. The fix adjusts the start bound to the beginning of the row instead, and splits the range tombstone so that the intra-row portion is presented to `CellGrouper` as a row tombstone while the remainder is deferred via a new push-back queue in `AtomIterator`.

### Before / After

```java
// BEFORE — in LegacyRangeTombstone constructor
if (start.collectionName == null)
    stop = new LegacyBound(stop.bound, stop.isStatic, null);
else
    start = new LegacyBound(start.bound, start.isStatic, null);
```

```java
// AFTER — bounds reanchored to row boundaries
if (start.collectionName == null)
    stop = new LegacyBound(Slice.Bound.inclusiveEndOf(stop.bound.values), stop.isStatic, null);
else
    start = new LegacyBound(Slice.Bound.inclusiveStartOf(start.bound.values), start.isStatic, null);
```

---

## How to Spot

Any code that derives a row boundary from a tombstone bound whose values were originally associated with a sub-row element (collection column) needs to be stripped of that sub-row context and anchored cleanly to the row's start or end. When tombstone splitting is needed but the iterator only supports forward traversal, a priority-queue push-back mechanism is required to keep atoms in order; absence of such a mechanism is the deeper structural gap.

---

## Test

`MixedModeRangeTombstoneTest#multiRowsRangeTombstoneAndCollectionTombstoneInteractionTest` runs a 2.2-to-3.0 upgrade scenario with the exact pattern — a multi-row DELETE, an INSERT, and a collection DELETE on the same row — and asserts the query returns exactly one row after upgrade.
