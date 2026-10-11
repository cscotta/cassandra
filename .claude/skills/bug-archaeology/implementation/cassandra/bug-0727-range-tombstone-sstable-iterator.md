# Bug 0727: Range tombstone boundary comparison and multi-block reverse iteration bugs

**Commit**: a85eeefe88 · **Ticket**: CASSANDRA-13340
**Subsystem**: SSTable iterators / storage engine
**Category**: Logic error in condition

---

## Root Cause
Three related bugs in the SSTable iterator code all caused incorrect handling of range tombstones, particularly during reverse (DESC) reads and when a range tombstone spans multiple index blocks.

1. **Off-by-one boundary comparison in `SSTableIterator`**: `computeNext()` used `deserializer.compareNextTo(end) > 0` to stop reading at a slice boundary. The correct comparison is `>= 0` because an unfiltered item equal to the slice end bound must be excluded — it might be an `EXCL_END` marker that coincidentally equals an `INCL_START` of the next slice, and returning it would violate slice semantics. The same strict-inequality bug existed in the indexed `hasNextInternal` path.

2. **Wrong `Kind.compare` call in `ClusteringPrefix`**: Bound-kind ordering between range tombstone markers and clustering keys used `nextKind.compareTo(bound.kind())` (enum natural order) instead of `Kind.compare(nextKind, bound.kind())` (semantics-aware comparison). The enum ordinal order does not match the intended sort order for all kind pairs, so range tombstone open/close markers were sorted incorrectly relative to clustering values.

3. **`SSTableReversedIterator` multi-block range tombstone duplication/omission**: When a range tombstone spans two index blocks, the reversed reader synthesizes artificial open/close bound markers at each block boundary. Previously the code lacked `skipFirstIteratedItem` / `skipLastIteratedItem` flags, causing the boundary marker added for a previous block to be emitted again when the adjacent block was read, resulting in duplicate or orphaned range tombstone bounds. A `SkipLastIterator` wrapper was introduced to suppress the redundant last element when needed.

### Before / After
```java
// BEFORE — SSTableIterator: wrong strict inequality
if (!deserializer.hasNext() || deserializer.compareNextTo(end) > 0)
    return null;

// AFTER
if (!deserializer.hasNext() || deserializer.compareNextTo(end) >= 0)
    return null;

// BEFORE — ClusteringPrefix: enum natural order
return nextKind.compareTo(bound.kind());

// AFTER — semantics-aware compare
return Kind.compare(nextKind, bound.kind());

// BEFORE — reversed multi-block: no skip flags; boundary markers always emitted
if (openMarker != null) {
    buffer.add(new RangeTombstoneBoundMarker(markerStart, openMarker));
    // no suppression for cross-block case
}

// AFTER — skip flags suppress duplicate emission across block boundaries
if (openMarker != null) {
    buffer.add(new RangeTombstoneBoundMarker(markerStart, openMarker));
    if (hasNextBlock)
        skipLastIteratedItem = true;
}
```

---

## How to Spot
Range tombstone boundaries require semantics-aware comparators, not enum ordinal comparators. Whenever `Kind.compareTo` (inherited from `Enum`) is used to order bound kinds, verify that the desired semantics match the enum declaration order. For slice boundary stop conditions, confirm whether the comparison should be strict (`> 0`) or non-strict (`>= 0`) by reasoning about what an item equal to the bound actually represents. Multi-block reverse readers are particularly risky when they inject synthetic boundary markers: trace through whether each marker is added once to the buffer and emitted exactly once to the caller for all combinations of hasPreviousBlock / hasNextBlock.

---

## Test
`DeleteTest` gains tests that create and query range tombstones spanning partition boundaries with both forward and reverse reads, verifying that the correct rows are returned and no duplicate or missing tombstone markers are produced.
