# Bug 0270: RangeMap Iterator Skips Entries When Two Peers Have Overlapping Ranges

**Commit**: 29c0ad80c9 · **Ticket**: CASSANDRA-17900
**Subsystem**: src/java/org/apache/cassandra/repair/asymmetric/RangeMap.java
**Category**: Logic error in condition

---

## Root Cause

`RangeMap`'s `intersectingEntryIterator` uses a chain of per-bucket iterators. The `computeNext` method advanced to the next bucket only if the current iterator was exhausted, but it used an `if` rather than a `while`. When the current iterator was exhausted and the next bucket's iterator was also immediately exhausted (e.g. a bucket covering a wrapped range that produced no intersecting results), the method incorrectly returned `endOfData()` without checking the remaining buckets, causing some entries to be silently skipped. During asymmetric repair, this meant overlapping mismatching ranges were invisible to the diff calculation, ultimately triggering an assertion failure.

### Before / After

```java
// BEFORE – single if; may return endOfData() prematurely when next bucket is also empty
if ((currentIter == null || !currentIter.hasNext()) && iterators.hasNext())
    currentIter = iterators.next();
if (currentIter != null && currentIter.hasNext())
    return currentIter.next();
return endOfData();
```

```java
// AFTER – loop until a non-empty bucket is found or all buckets exhausted
while (currentIter == null || !currentIter.hasNext())
{
    if (!iterators.hasNext())
        return endOfData();
    currentIter = iterators.next();
}
return currentIter.next();
```

---

## How to Spot

Any iterator that chains multiple sub-iterators and advances with a single `if` instead of a `while` is susceptible to this bug: an empty sub-iterator causes early termination rather than continued advancement. Code review should require that "advance to next sub-iterator" logic keeps looping until a non-empty one is found or the chain is exhausted.

---

## Test

`RangeMapTest.testWrap` verifies that `removeIntersecting` with a wrapping range correctly empties a map that contained a non-wrapping range.
