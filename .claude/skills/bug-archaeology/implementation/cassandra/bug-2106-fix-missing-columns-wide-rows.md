# Bug 2106: Missing Columns in Wide Row Queries Due to Swapped Index Ranges

**Commit**: 2fe8133bbe · **Ticket**: CASSANDRA-5225
**Subsystem**: src/java/org/apache/cassandra/io/sstable/IndexHelper.java
**Category**: Logic error in condition

---

## Root Cause

`IndexHelper.indexFor` locates the correct column index block for a query using a binary search. When a `lastIndex` hint was provided (to resume scanning partway through an index list), the code applied the wrong sublist slice to forward vs. reversed scans: the slice meant for forward scans (`subList(0, lastIndex+1)`) was used for reversed scans, and the slice meant for reversed scans (`subList(lastIndex, size)`) was used for forward scans. This caused binary search to operate on the wrong portion of the index list, returning an incorrect block index and silently skipping columns in wide rows.

### Before / After

```java
// BEFORE
if (reversed)
{
    startIdx = lastIndex;
    toSearch = indexList.subList(lastIndex, indexList.size());
}
else
{
    toSearch = indexList.subList(0, lastIndex + 1);
}
```

```java
// AFTER
if (reversed)
{
    toSearch = indexList.subList(0, lastIndex + 1);
}
else
{
    startIdx = lastIndex;
    toSearch = indexList.subList(lastIndex, indexList.size());
}
```

---

## How to Spot

When two sibling branches of a conditional each set a different variable (one sets `startIdx`, the other does not), double-check that the logic in each branch is not accidentally transposed. Asymmetric-looking if/else blocks are a red flag for copy-paste swaps.

---

## Test

Existing `IndexHelperTest` assertions were updated to reflect the corrected return values for both forward and reversed scans with a `lastIndex` hint.
