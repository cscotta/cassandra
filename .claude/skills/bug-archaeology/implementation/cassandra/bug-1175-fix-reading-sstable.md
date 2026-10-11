# Bug 1175: Incorrect Comparator and Missing Row Marker When Reading 2.1 SSTables

**Commit**: f713be4aa2 · **Ticket**: n/a (CASSANDRA-9705 followup)
**Subsystem**: src/java/org/apache/cassandra/db/LegacyLayout.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The 3.0 legacy SSTable reader for 2.1-format files contained several inter-related defects introduced by the CASSANDRA-9705 storage engine refactor. First, `toUnfilteredRowIterator` merged cells and range tombstones using `metadata.comparator` directly on `LegacyAtom` objects, but `LegacyAtom` no longer implemented `Clusterable`, so comparisons fell back to incorrect object identity or threw a compile error. The correct comparator needed to handle the special cases of collection tombstones (which sort just before their column, not before the whole row) and row-deletion tombstones, requiring a dedicated `legacyAtomComparator`. Second, the static-row builder path never called `builder.newRow(Clustering.STATIC_CLUSTERING)` before adding static cells, so the first static cell's value was discarded and subsequent cells could be attributed to the wrong row. Third, secondary-index tables are stored as compound-but-dense tables with no regular columns; when `decodeCellName` encountered a null column component in this configuration, it threw `IllegalArgumentException` instead of returning a row marker. Fourth, `ArrayBackedRow.Builder.newRow()` checked `cells.isEmpty()` as its guard against double-initialisation, but calling `newRow` twice with an empty cell list (as was possible in the static path) silently re-initialised the builder.

### Before / After

```java
// BEFORE: wrong comparator type passed to merge iterator
Iterator<LegacyAtom> merged = MergeIterator.get(
    iterators, grouper.metadata.comparator, reducer);
```

```java
// AFTER: dedicated comparator handles cell/tombstone ordering edge cases
Iterator<LegacyAtom> merged = MergeIterator.get(
    iterators, legacyAtomComparator(grouper.metadata), reducer);
```

```java
// BEFORE: static row builder started without initialising clustering
Row.Builder builder = ArrayBackedRow.unsortedBuilder(statics, FBUtilities.nowInSeconds());
// ... immediately starts adding static cells ...
```

```java
// AFTER: static clustering set before any cells are added
Row.Builder builder = ArrayBackedRow.unsortedBuilder(statics, FBUtilities.nowInSeconds());
builder.newRow(Clustering.STATIC_CLUSTERING);
```

---

## How to Spot

Followup patches to large refactors are prime territory for this class of bug. When an interface is restructured (here `LegacyAtom` losing `Clusterable`), any caller that passed objects of that type to a generic comparator silently shifts from a correct comparison to an incorrect one. Code review should verify that every generic algorithm (merge iterators, sort comparators) receives a comparator that was written for the specific type in use, not for a supertype or a structurally similar but semantically different type. Builder objects with multiple entry points (`newRow`, `addCell`) should guard each path independently rather than relying on implicit preconditions that can become stale across refactors.

---

## Test

No dedicated test added in this commit; the fix was validated by running existing integration tests against 2.1 SSTable fixtures.
