# Bug 1141: Incorrect Results for Repeated Slices on AbstractThreadUnsafePartition

**Commit**: e5e2910443 · **Ticket**: CASSANDRA-10002
**Subsystem**: src/java/org/apache/cassandra/db/partitions/AbstractThreadUnsafePartition.java
**Category**: Off-by-one

---

## Root Cause

`SliceableIterator` performed binary searches to locate the start and end of each slice within the internal row array. The `search()` method accepted a `from` offset so that subsequent slices could skip already-visited rows. However, the code did not account for the fact that `search()` returns a position *relative to the `from` offset*, not an absolute index into the array. Consequently, after the first slice the computed `start` and `end` indices were too small by `nextIdx` / `start` respectively, causing the iterator to either return no rows or return rows from the wrong position in the partition.

### Before / After

```java
// BEFORE
final int start = -search(slice.start(), nextIdx, rows.size()) - 1;
// ...
final int end = -search(slice.end(), start, rows.size()) - 1;
```

```java
// AFTER
int searchResult = search(slice.start(), nextIdx, rows.size());
final int start = nextIdx + (-searchResult - 1);
// ...
searchResult = search(slice.end(), start, rows.size());
final int end = start + (-searchResult - 1);
```

---

## How to Spot

Whenever a binary search accepts a non-zero `fromIndex` parameter, check whether the returned value is absolute or relative to that base. The standard `Arrays.binarySearch(array, fromIndex, toIndex, key)` returns an absolute index, but custom implementations may return a relative one. Callers that add back the base for only one of several search calls, or that omit the addition entirely, will produce wrong offsets for all but the first invocation.

---

## Test

No new test was added in this commit; the fix was covered by the existing slice iteration test suite.
