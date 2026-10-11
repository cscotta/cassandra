# Bug 1404: Fix ordering on SSTableScanner / SSTableReader close

**Commit**: 3484181e2b · **Ticket**: CASSANDRA-8019
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableScanner.java
**Category**: Resource leak

---

## Root Cause

`SSTableScanner` opened data and index file readers against an `SSTableReader` without acquiring a reference count on the reader. The `SSTableReader` reference-counting mechanism (`acquireReference` / `releaseReference`) exists to prevent the underlying files from being deleted while a scan is still in progress. Without the acquire/release pair, a concurrent compaction could complete and mark the SSTable for deletion while a scanner was actively reading it, leading to file-not-found errors or, more subtly, reading from a partially-deleted file. The `close()` method also did not call `releaseReference`, so even if reference counting had been acquired elsewhere the scanner would have leaked it.

### Before / After

```java
// BEFORE
SSTableScanner(SSTableReader sstable, DataRange dataRange, RateLimiter limiter)
{
    assert sstable != null;
    this.dfile = ...sstable.openDataReader()...;
    this.ifile = sstable.openIndexReader();
    ...
}

public void close() throws IOException
{
    FileUtils.close(dfile, ifile);
    // no releaseReference
}
```

```java
// AFTER
SSTableScanner(SSTableReader sstable, DataRange dataRange, RateLimiter limiter)
{
    assert sstable != null;
    sstable.acquireReference();          // hold ref for lifetime of scan
    this.dfile = ...sstable.openDataReader()...;
    this.ifile = sstable.openIndexReader();
    ...
}

public void close() throws IOException
{
    FileUtils.close(dfile, ifile);
    sstable.releaseReference();          // allow deletion after scan ends
}
```

---

## How to Spot

Any component that holds a long-lived reference to an `SSTableReader` and performs I/O operations against it must bracket the lifetime with `acquireReference` / `releaseReference`. Reviewers should audit every constructor or factory method that accepts an `SSTableReader` and check whether reference acquisition appears before the first I/O call and release appears in `close()` or a `finally` block. The test suite update, which wraps scanners in try-with-resources, is also a reliable signal that the original code lacked proper cleanup.

---

## Test

`SSTableRewriterTest` updated to wrap scanners and controllers in try-with-resources, exercising the acquire/release lifecycle across multiple compaction-rewriting scenarios including abort and truncate paths.
