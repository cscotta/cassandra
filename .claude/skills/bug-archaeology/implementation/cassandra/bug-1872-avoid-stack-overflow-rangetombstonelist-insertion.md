# Bug 1872: Avoid Stack Overflow on RangeTombstoneList Insertion

**Commit**: 4768daa9d0 · **Ticket**: CASSANDRA-6181
**Subsystem**: src/java/org/apache/cassandra/db/RangeTombstoneList.java
**Category**: Logic error in condition

---

## Root Cause

The original `insertFrom` / `weakInsertFrom` / `insertAfter` implementation used mutual recursion to handle overlapping range tombstone insertions. For each overlapping interval encountered, `weakInsertFrom` called `insertFrom`, which could call `insertAfter`, which called `weakInsertFrom` again for the next element. Inserting a single large tombstone that spans thousands of pre-existing intervals therefore produced a call chain proportional to the number of intervals, easily blowing the JVM stack. Additionally, the binary search and insertion-point logic was incorrectly comparing against `starts[]` instead of `ends[]`, causing wrong interval boundaries.

### Before / After

```java
// BEFORE – recursive helper chain; weakInsertFrom calls insertFrom recursively
private void weakInsertFrom(int i, ByteBuffer start, ByteBuffer end, long markedAt, int delTime)
{
    if (i == size - 1 || comparator.compare(start, starts[i+1]) < 0)
        insertFrom(i, start, end, markedAt, delTime);
    else
        weakInsertFrom(i+1, start, end, markedAt, delTime);  // unbounded recursion
}

// Binary search used starts[] as reference
int pos = Arrays.binarySearch(starts, 0, size, start, comparator);
```

```java
// AFTER – single iterative loop in insertFrom, no recursion
private void insertFrom(int i, ByteBuffer start, ByteBuffer end, long markedAt, int delTime)
{
    while (i < size)
    {
        // ... iterative overlap resolution using ends[] as the reference boundary
        start = ends[i];
        i++;
    }
    addInternal(i, start, end, markedAt, delTime);
}

// Binary search now uses ends[] as reference
int pos = Arrays.binarySearch(ends, 0, size, start, comparator);
```

---

## How to Spot

Any recursive method that advances an index by one element per call on a potentially large collection is susceptible to stack overflow. During review, look for recursive helpers that iterate rather than divide-and-conquer; convert them to iterative loops.

---

## Test

`largeAdditionTest` creates 3 000 non-overlapping intervals and then adds a single range tombstone spanning all of them, asserting no `StackOverflowError` and that the final list has the correct size. `overlappingPreviousEndEqualsStartTest` verifies that adjacent intervals (where `end[i] == start[i+1]`) are resolved correctly.
