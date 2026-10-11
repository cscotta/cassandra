# Bug 1865: Fixes for compacting larger-than-memory rows

**Commit**: d4b5b0dbc5 · **Ticket**: CASSANDRA-6274
**Subsystem**: db/compaction, db/ColumnIndex
**Category**: Logic error in condition

---

## Root Cause

`LazilyCompactedRow` called `controller.shouldPurge()` with `maxDelTimestamp` computed only from
row-level deletion info gathered during construction, before iterating column cells.  Because
cell-level tombstones newer than the row-level tombstones would not be seen until iteration, the
purge decision was based on an artificially low timestamp, causing live data to be incorrectly
purged in some cases.  Additionally, the old `ColumnIndex.Builder.build(Iterable)` always called
`finish()` (which writes an empty-row header) even when compaction resulted in a fully-expired row
that should be dropped, producing corrupt output for larger-than-memory rows.  The `Scrubber` also
held `AbstractCompactedRow` references for out-of-order detection, preventing GC of large rows that
had already been processed.

### Before / After

```java
// BEFORE – shouldPurge called with only row-level tombstone timestamp
this.shouldPurge = controller.shouldPurge(key, maxDelTimestamp);
```

```java
// AFTER – pass MAX_VALUE so purge only happens when no other version exists
this.shouldPurge = controller.shouldPurge(key, Long.MAX_VALUE);
```

```java
// BEFORE – build() always writes empty row header via finish()
public ColumnIndex build(Iterable<OnDiskAtom> columns) throws IOException {
    for (OnDiskAtom c : columns) add(c);
    ColumnIndex index = build();
    finish();
    return index;
}
```

```java
// AFTER – caller decides whether to write empty header
public ColumnIndex buildForCompaction(Iterator<OnDiskAtom> columns) throws IOException {
    while (columns.hasNext()) add(columns.next());
    return build();
}
// ...caller later invokes:
indexBuilder.maybeWriteEmptyRowHeader();
```

---

## How to Spot

When a purge/drop decision is made before all contributing data has been consumed (e.g., reading
deletion metadata from multiple sources eagerly while deferring column iteration), verify that
the timestamp used for the decision accounts for the maximum possible tombstone timestamp across
all data sources, not just the subset available at construction time.  Any `shouldPurge`-style
call made before full iteration is a red flag.

---

## Test

No test added (test data serialization binary updated as a side-effect).
