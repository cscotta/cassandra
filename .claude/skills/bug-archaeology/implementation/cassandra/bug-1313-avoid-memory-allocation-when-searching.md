# Bug 1313: Avoid Memory Allocation When Searching Index Summary

**Commit**: bf9c503130 · **Ticket**: CASSANDRA-8793
**Subsystem**: src/java/org/apache/cassandra/io/sstable/IndexSummary.java
**Category**: Missing override of default method

---

## Root Cause

`IndexSummary#binarySearch()` called `ByteBuffer.wrap(getKey(mid))` on every iteration of the binary search loop, allocating a new heap `ByteBuffer` for each comparison. Because the index summary lives in off-heap `Memory`, wrapping the key bytes into a heap buffer forced an unnecessary copy on each probe. The fix introduces a "hollow" direct `ByteBuffer` — a reusable instance whose address and length are updated in-place via `Unsafe` — eliminating the per-iteration allocation. Additionally, `Memory.setShort` had a bounds-check typo (`offset + 4` instead of `offset + 2`), and `SafeMemoryWriter` was missing `@Override` annotations on several methods that shadow superclass implementations, hiding the fact that one method signature (`write(Memory)`) had drifted from the interface contract.

### Before / After

```java
// BEFORE
result = -DecoratedKey.compareTo(partitioner, ByteBuffer.wrap(getKey(mid)), key);
```

```java
// AFTER
fillTemporaryKey(mid, hollow);   // reuses a single pre-allocated hollow ByteBuffer
result = -DecoratedKey.compareTo(partitioner, hollow, key);
```

---

## How to Spot

Binary search and other hot-path loops that construct a new wrapper object (especially `ByteBuffer.wrap(...)`) around off-heap or already-materialized data on every iteration are candidates for this pattern. A reviewer should ask: can the wrapper be pre-allocated outside the loop and its view updated in-place? The missing `@Override` annotations are a secondary signal — when an interface or abstract class is refactored, implementations without `@Override` silently stop overriding the updated signature, breaking dispatch.

---

## Test

No test added.
