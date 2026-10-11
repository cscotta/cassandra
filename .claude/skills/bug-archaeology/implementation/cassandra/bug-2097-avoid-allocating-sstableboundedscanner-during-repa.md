# Bug 2097: SSTableBoundedScanner Allocated Even When Range Does Not Intersect SSTable

**Commit**: a4166496f2 · **Ticket**: CASSANDRA-5249
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java, src/java/org/apache/cassandra/io/sstable/SSTableBoundedScanner.java, src/java/org/apache/cassandra/db/compaction/LeveledCompactionStrategy.java
**Category**: Resource leak

---

## Root Cause

`SSTableBoundedScanner` was constructed before checking whether the requested range actually intersects the SSTable. The constructor computed positions for the range and, if no positions were found, set `exhausted = true` — but the scanner object (and its file handle) had already been fully allocated. The fix moves the range-intersection check to the factory site in `SSTableReader.getDirectScanner`, returning a lightweight `EmptyCompactionScanner` when there is no overlap, so that no file resources are opened unnecessarily.

### Before / After

```java
// BEFORE (SSTableBoundedScanner constructor)
SSTableBoundedScanner(SSTableReader sstable, boolean skipCache, Range<Token> range)
{
    super(sstable, skipCache);
    this.rangeIterator = sstable.getPositionsForRanges(...).iterator();
    if (rangeIterator.hasNext()) { ... }
    else { exhausted = true; }
}
```

```java
// AFTER (SSTableReader.getDirectScanner)
Iterator<Pair<Long, Long>> rangeIterator = getPositionsForRanges(...).iterator();
return rangeIterator.hasNext()
       ? new SSTableBoundedScanner(this, true, rangeIterator)
       : new EmptyCompactionScanner(getFilename());
```

---

## How to Spot

When a constructor's only job in the non-intersecting case is to set `exhausted = true` after allocating file handles, the intersection check should be lifted to the call site. Constructors that can produce a logically empty/no-op object from shared resources are candidates for refactoring into factory methods that can return a cheaper sentinel.

---

## Test

No test added.
