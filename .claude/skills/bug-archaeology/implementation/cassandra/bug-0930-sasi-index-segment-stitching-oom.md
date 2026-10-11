# Bug 0930: SASI Index Segment Stitching in RAM Causes OOM

**Commit**: 5c4d5c731f · **Ticket**: CASSANDRA-11383
**Subsystem**: SASI secondary index
**Category**: Resource leak

---

## Root Cause
The original `TokenTreeBuilder` held all token-to-row-id mappings in a single in-memory `TreeMap` and merged ("stitched") all per-segment index data together in RAM before writing it to disk. For large SSTables with many indexed terms, this caused the heap to be exhausted (OOM). The fix splits `TokenTreeBuilder` into two specialisations: `DynamicTokenTreeBuilder` (used during index construction, backed by an in-memory structure) and `StaticTokenTreeBuilder` (used when reading an already-serialized segment back for merging). The on-disk merge in `PerSSTableIndexWriter` now streams segment data directly into the final output file rather than accumulating it in memory, and the `MutableDataBlock` / `DataBuilderLevel` classes are updated to use `DynamicTokenTreeBuilder` for all intermediate structures so that each is bounded in size and can be flushed independently.

### Before / After
```java
// BEFORE — single unbounded in-memory builder, all data stitched in RAM
terms.put(term, (tokens = new TokenTreeBuilder()));
...
superBlockTree = new TokenTreeBuilder();

// AFTER — bounded dynamic builder, flushed incrementally
terms.put(term, (tokens = new DynamicTokenTreeBuilder()));
...
superBlockTree = new DynamicTokenTreeBuilder();
```

---

## How to Spot
Index builders that accumulate all data in a single in-memory structure before writing are vulnerable to OOM on large SSTables. During review, look for data structures that grow unboundedly proportional to SSTable size (e.g., a `TreeMap` keyed on every indexed term) and verify the builder has a flush/spill mechanism. Also check that on-disk merge paths stream data rather than loading all segments into the heap simultaneously.

---

## Test
`PerSSTableIndexWriterTest` was added to cover multi-segment index writing; `OnDiskIndexTest` and `TokenTreeTest` were updated to reflect the new builder hierarchy.
