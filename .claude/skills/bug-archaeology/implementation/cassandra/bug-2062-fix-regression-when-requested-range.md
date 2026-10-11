# Bug 2062: SSTableBoundedScanner Crashes When Range Misses SSTable

**Commit**: e306a87b7d · **Ticket**: CASSANDRA-5407
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java, src/java/org/apache/cassandra/io/sstable/SSTableBoundedScanner.java
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableReader.getScanner(Range)` unconditionally constructed an `SSTableBoundedScanner`, which in its constructor called `rangeIterator.next()` to set the initial seek position. If the requested token range did not overlap the SSTable at all, `getPositionsForRanges()` returned an empty list and the iterator had no elements, causing a `NoSuchElementException` on the first `next()` call. The fix checks whether the position iterator has any elements before constructing a scanner: if the iterator is empty an `EmptyCompactionScanner` (a new no-op implementation) is returned instead, and the `SSTableBoundedScanner` constructor now asserts that its iterator is non-empty.

### Before / After

```java
// BEFORE
public ICompactionScanner getScanner(Range<Token> range) {
    if (range == null) return getScanner();
    return new SSTableBoundedScanner(this, range); // crashes if no overlap
}
```

```java
// AFTER
public ICompactionScanner getScanner(Range<Token> range) {
    if (range == null) return getScanner();
    Iterator<Pair<Long,Long>> rangeIterator =
        getPositionsForRanges(Collections.singletonList(range)).iterator();
    if (rangeIterator.hasNext())
        return new SSTableBoundedScanner(this, rangeIterator);
    else
        return new EmptyCompactionScanner(getFilename());
}
```

---

## How to Spot

Any scanner or iterator that is initialized by calling `next()` in its constructor is fragile when the underlying data may be empty. Always gate construction on `hasNext()`, or push the eagerness into the constructor with a clear assertion, and ensure callers can receive a safe no-op object for the empty case.

---

## Test

`SSTableReaderTest.testGetScannerForNoIntersectingRanges` — writes one row, flushes, then calls `getScanner` with a range guaranteed not to contain that key and asserts that the scanner can be traversed without throwing.
