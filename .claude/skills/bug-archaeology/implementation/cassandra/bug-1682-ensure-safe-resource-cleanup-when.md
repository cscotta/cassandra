# Bug 1682: Ensure Safe Resource Cleanup When Replacing SSTables

**Commit**: 5ebadc11e3 · **Ticket**: CASSANDRA-6912
**Subsystem**: src/java/org/apache/cassandra/io/sstable, src/java/org/apache/cassandra/db
**Category**: Resource leak

---

## Root Cause

When an `SSTableReader` was replaced by a new instance sharing the same underlying files (for example after index summary downsampling), the old `isReplaced` flag approach could not safely determine which resources — bloom filter, index summary, data/index file handles — were shared versus exclusive. Closing the old reader could skip cleanup of resources that the new reader did not actually share, leaking them. Conversely, closing shared resources prematurely would corrupt the replacement reader.

The fix introduces a doubly-linked replacement chain (`replaces`/`replacedBy`) protected by a shared `replaceLock`. At close time, each reader inspects both neighbors and only closes a resource (bloom filter, index summary, file handles) if it is not referenced by either adjacent link, ensuring exactly-once cleanup regardless of how many replacements have occurred.

### Before / After

```java
// BEFORE: single boolean flag, all resources skipped if replaced
private final AtomicBoolean isReplaced = new AtomicBoolean(false);
...
if (!isReplaced.get())
{
    ifile.cleanup();
    dfile.cleanup();
    bf.close();
}
```

```java
// AFTER: linked-list chain compares actual object identity
synchronized (replaceLock)
{
    boolean closeBf      = replacedBy == null || replacedBy.bf != bf;
    boolean closeSummary = replacedBy == null || replacedBy.indexSummary != indexSummary;
    boolean closeFiles   = replacedBy == null || replacedBy.dfile != dfile;
    if (replaces != null)
    {
        closeBf      &= replaces.bf != bf;
        closeSummary &= replaces.indexSummary != indexSummary;
        closeFiles   &= replaces.dfile != dfile;
    }
    // only close each resource when no neighbor holds the same reference
}
```

---

## How to Spot

A boolean "replaced" flag cannot capture which specific resources were shared with the replacement. Whenever a class reuses some but not all resources from a predecessor, each resource must carry its own close guard (or share a reference count). Look for resource-owning classes with a single "is replaced" boolean that governs all cleanup paths uniformly.

---

## Test

Existing `SSTableReaderTest` was updated to exercise the replacement path; `IndexSummaryManagerTest` adjusted accordingly.
