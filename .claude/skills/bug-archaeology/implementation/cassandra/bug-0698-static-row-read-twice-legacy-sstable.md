# Bug 0698: Static Row Read Twice from Legacy SSTable

**Commit**: 415d06b1da · **Ticket**: CASSANDRA-13236
**Subsystem**: src/java/org/apache/cassandra/db/columniterator/AbstractSSTableIterator.java
**Category**: Logic error in condition

---

## Root Cause

In old-format (pre-3.0) SSTables, the static row is stored at the head of the partition data and is read once when the iterator is first opened. When a slice reader subsequently seeks back to block index 0 (the start of the partition), it repositions the file cursor to the beginning of the partition but does not skip the static row that was already consumed during initialization. The deserializer then reads the static row a second time and, because it treats it as a regular row, encounters invalid flags — a deserialization error surfaces later as data corruption rather than an obvious duplicate-read error.

### Before / After

```java
// BEFORE — no skip after seeking to block 0 in old-format files
currentIndexIdx = blockIdx;
reader.openMarker = blockIdx > 0 ? indexes.get(blockIdx - 1).endOpenMarker : null;
mark = reader.file.mark();
```

```java
// AFTER — skip already-consumed static row when rewinding to block 0
mark = reader.file.mark();
if (!reader.version.storeRows()
    && blockIdx == 0
    && reader.deserializer.hasNext()
    && reader.deserializer.nextIsStatic())
{
    reader.deserializer.skipNext();
}
```

---

## How to Spot

Any seek/rewind path in a format-aware file iterator must account for what was already consumed during iterator construction. When a reader is initialised by reading a leading special row (e.g., a static row in the legacy format), that consumption leaves the file cursor past that row. A seek back to position 0 of the same block must replicate that initial skip; failure to do so effectively re-reads data the caller believes was already processed.

---

## Test

No test added in this commit (relates to prior work in CASSANDRA-12088).
