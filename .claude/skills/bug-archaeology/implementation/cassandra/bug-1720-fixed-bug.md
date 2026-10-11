# Bug 1720: Wrong Variable Used in CompactionManager.needsCleanup Loop

**Commit**: 05da045bbf · **Ticket**: CASSANDRA-6845
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

`needsCleanup` builds two lists: `ownedRanges` (ranges the node currently owns) and `sortedRanges` (the sorted set of ranges being scanned). The loop index bound check that decides whether the current token is past the last range used `ownedRanges.size()` instead of `sortedRanges.size()`. If the two lists have different sizes, the terminal condition fires at the wrong iteration, causing cleanup to be skipped or triggered incorrectly.

### Before / After

```java
// BEFORE
if (i == (ownedRanges.size() - 1))
{
    // we're at the last range and we found a key beyond the end of the range
    return true;
```

```java
// AFTER
if (i == (sortedRanges.size() - 1))
{
    // we're at the last range and we found a key beyond the end of the range
    return true;
```

---

## How to Spot

When a loop iterates over one collection (`sortedRanges`) but the boundary check references a different collection (`ownedRanges`), verify the two are always the same size. If their sizes can diverge, the wrong variable is being used.

---

## Test

No test added.
