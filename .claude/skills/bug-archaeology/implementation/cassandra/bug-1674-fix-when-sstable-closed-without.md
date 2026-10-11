# Bug 1674: Fix AssertionError When SSTable Is Closed Without Releasing Reference

**Commit**: 930905bbeb · **Ticket**: CASSANDRA-7000
**Subsystem**: db/ColumnFamilyStore, db/compaction/CompactionManager, io/sstable/SSTableReader
**Category**: Resource leak

---

## Root Cause

Several callers obtained SSTable handles via `getSSTables()` — which does not take a reference — but then used those handles for work that could race with compaction replacing the SSTable. `keySamples()` and `estimatedKeysForRange()` in `ColumnFamilyStore` never acquired references at all. In `CompactionManager`, snapshot-validation compactions called `FileUtils.closeQuietly()` on the readers instead of `releaseReferences()`, so the reference count never reached zero through the normal path; this triggered the `assert references.get() == 0` in `SSTableReader.close()` as an `AssertionError`. The fix changes all three sites to acquire references with `markCurrentSSTablesReferenced()` and release them in a `finally` block via `SSTableReader.releaseReferences()`, and converts the assertion to an `IllegalStateException` so the error is always reported even with assertions disabled.

### Before / After

```java
// BEFORE (ColumnFamilyStore)
Collection<SSTableReader> sstables = getSSTables();
// ... iterate sstables without holding a reference ...
// no release

// BEFORE (CompactionManager, snapshot validation path)
if (isSnapshotValidation)
{
    for (SSTableReader sstable : sstables)
        FileUtils.closeQuietly(sstable);   // wrong: closes instead of releasing
    cfs.clearSnapshot(snapshotName);
}
else
{
    SSTableReader.releaseReferences(sstables);
}
```

```java
// AFTER (ColumnFamilyStore)
Collection<SSTableReader> sstables = markCurrentSSTablesReferenced();
try { /* iterate */ }
finally { SSTableReader.releaseReferences(sstables); }

// AFTER (CompactionManager) — unified, always releases
finally
{
    iter.close();
    SSTableReader.releaseReferences(sstables);
    if (isSnapshotValidation)
        cfs.clearSnapshot(snapshotName);
}
```

---

## How to Spot

Any code that calls `getSSTables()` and then uses the returned readers past a potential yield point needs a matching `markCurrentSSTablesReferenced()` + `releaseReferences()` pair. Code reviews should flag bare `getSSTables()` calls in methods that perform I/O or block.

---

## Test

No test added.
