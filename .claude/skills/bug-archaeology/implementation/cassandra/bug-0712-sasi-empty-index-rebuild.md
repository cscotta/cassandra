# Bug 0712: SASI empty index file triggers unnecessary rebuild

**Commit**: 317a3ed627 · **Ticket**: CASSANDRA-12962
**Subsystem**: SASI secondary index
**Category**: Logic error in condition

---

## Root Cause
When `OnDiskIndexBuilder.finish()` found no terms to write it returned `false` without creating any index file on disk. On the next `DataTracker.update()` call the absence of a file caused the sstable to be omitted from the "already indexed" set, which in turn triggered a redundant rebuild of an index that was legitimately empty. The fix creates a zero-length sentinel file when there are no terms, and `DataTracker` now explicitly treats a zero-length file as "already built" — including the sstable in the covered set but not adding it to the active query index.

### Before / After
```java
// BEFORE — no sentinel file written; sstable dropped from covered set
if (terms.isEmpty())
    return false;

// DataTracker derived covered SSTables via a stream over SSTableIndex objects,
// so an sstable with no index object was never considered "built".

// AFTER — sentinel created; DataTracker returns covered set separately
if (terms.isEmpty())
{
    file.createNewFile();   // zero-length sentinel
    return false;
}

// In DataTracker.getBuiltIndexes():
if (indexFile.length() == 0)
{
    builtSSTables.add(sstable);   // mark as covered
    continue;                      // do not add to active index
}
```

---

## How to Spot
Look for index-rebuild logic that derives "already built" membership solely from which index objects were successfully loaded. If a legitimately-empty outcome produces no artifact at all (no file, no marker), the system cannot distinguish "never indexed" from "indexed and empty", causing spurious work on every restart or compaction cycle. Always ensure that a completed build leaves an observable side-effect even when the result set is empty.

---

## Test
`SASIIndexTest` extended with a case that inserts rows with no indexable values for a SASI column, flushes, and verifies the index is not rebuilt on subsequent `DataTracker.update()` calls.
