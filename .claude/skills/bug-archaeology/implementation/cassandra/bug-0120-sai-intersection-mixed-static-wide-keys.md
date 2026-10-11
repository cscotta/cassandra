# Bug 0120: SAI Intersection Under-Skipping with Mixed STATIC and WIDE Keys

**Commit**: a118a704cb · **Ticket**: CASSANDRA-20258
**Subsystem**: src/java/org/apache/cassandra/index/sai/iterators
**Category**: Logic error in condition

---

## Root Cause

`KeyRangeIntersectionIterator` uses a "skip-to-highest-key" strategy to advance lagging iterators during an intersection. When the current iterator's peek was a STATIC key and `highestKey` was WIDE, the code correctly skipped to the static form of the partition (`highestKey.toStatic()`). However, it never checked whether the iterator—which can contain a mix of STATIC and WIDE postings—had already advanced past that static position into a WIDE posting that still sorted *below* `highestKey`. In that case the iterator was left pointing at a key that compared less than `highestKey` (strict), causing the intersection to omit matching results (under-skipping rather than over-skipping).

A secondary issue in `StorageAttachedIndexGroup.insertRow` caused deletion tombstones to be indexed even though SAI does not process them; those phantom postings created the mixed STATIC/WIDE posting lists that triggered the skipping fault.

### Before / After

```java
// BEFORE – stops at the partition's STATIC boundary, never advances further
PrimaryKey nextKey = range.peek().kind() == Kind.STATIC
                     ? skipAndPeek(range, highestKey.toStatic())
                     : skipAndPeek(range, highestKey);
```

```java
// AFTER – if a WIDE key landed below highestKey after the STATIC skip, advance again
private PrimaryKey skipToHighestKey(KeyRangeIterator range)
{
    if (range.peek().kind() == highestKey.kind())
        return skipAndPeek(range, highestKey);

    if (range.peek().kind() == Kind.STATIC)
    {
        PrimaryKey nextKey = skipAndPeek(range, highestKey.toStatic());
        if (nextKey != null && nextKey.compareTo(highestKey, true) < 0 && nextKey.kind() == Kind.WIDE)
            nextKey = skipAndPeek(range, highestKey);
        return nextKey;
    }
    return skipAndPeek(range, highestKey);
}
```

---

## How to Spot

When an iterator abstraction has two distinct orderings or key kinds (e.g. STATIC vs. WIDE) that interleave in a posting list, any "skip-to-target" logic must be checked for the case where a single skip lands between the two kinds at the same partition—i.e., it overshoots the static boundary but undershoots the intended wide position. Review all early-exit conditions after a skip to confirm the result still satisfies the original comparison invariant under every possible key kind combination.

---

## Test

`CompositePartitionKeyIndexTest.testIntersectionOnMixedPostingsOnDelete` inserts rows plus a deletion (which generates a mixed posting list), flushes, then asserts that a two-column SAI intersection (`pk0 = false AND ck0 = '...'`) returns exactly the surviving wide row and not phantom static or deleted rows.
