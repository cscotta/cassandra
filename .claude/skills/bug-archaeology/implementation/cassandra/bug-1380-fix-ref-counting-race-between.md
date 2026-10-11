# Bug 1380: Reference Counting Race Between SSTableScanner and SSTableReader

**Commit**: 1fec4a4281 · **Ticket**: CASSANDRA-8399
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableScanner.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`SSTableScanner` constructors called `sstable.acquireReference()` after entering the constructor body, but the SSTable could be concurrently marked compacted and have its reference count decremented to zero between the point the caller retrieved the reader reference and the point `acquireReference()` ran inside the constructor. If the SSTable was deleted during that window, the scanner held a reference to a freed or deleted reader, potentially leading to reads of invalid data or a reference count going negative. The `ICompactionScanner` interface was also mixed together with the empty-scanner sentinel, making the type system unable to distinguish a valid scanner from a no-op placeholder.

The fix moves reference acquisition into a static factory method (`getScanner`) that atomically checks `acquireReference()` and returns an `EmptySSTableScanner` if the table is already gone, so a scanner is never constructed against an unreferenced reader.

### Before / After

```java
// BEFORE: acquireReference inside constructor, after the race window
SSTableScanner(SSTableReader sstable, DataRange dataRange, RateLimiter limiter)
{
    assert sstable != null;
    sstable.acquireReference();   // too late — sstable may already be deleted
    this.dfile = ...
```

```java
// AFTER: factory checks reference before constructing scanner
public static ISSTableScanner getScanner(SSTableReader sstable, DataRange dataRange, RateLimiter limiter)
{
    return sstable.acquireReference()
            ? new SSTableScanner(sstable, dataRange, limiter)
            : new SSTableScanner.EmptySSTableScanner(sstable.getFilename());
}
```

---

## How to Spot

Whenever a constructor acquires a reference to a shared, ref-counted resource, ask whether the caller's decision to call that constructor was based on a stale observation of the resource's availability. If another thread can transition the resource to an unavailable state between the observation and the `acquireReference` call, a TOCTOU race exists. The fix is always to combine the check and the acquisition atomically in a factory method that can return a safe sentinel rather than throwing or crashing.

---

## Test

`SSTableScannerTest`, `SSTableRewriterTest`, and several compaction tests were updated to use the new factory method and `ISSTableScanner` interface.
