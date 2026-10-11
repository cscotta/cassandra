# Bug 0653: SASI TokenTree serializedSize Omits Overflow Collision Bytes for Single Leaf

**Commit**: 436761ed6a · **Ticket**: CASSANDRA-13869
**Subsystem**: src/java/org/apache/cassandra/index/sasi/disk/AbstractTokenTreeBuilder.java
**Category**: Off-by-one

---

## Root Cause

`AbstractTokenTreeBuilder.serializedSize()` has a fast path for the case where the entire token tree fits in a single leaf block. That fast path calculated size as `BLOCK_HEADER_BYTES + tokenCount * 16` (where 16 was a hardcoded byte count per entry), but it did not account for the overflow-collision section that is appended after the regular entries when multiple row IDs hash to the same token. Multi-block trees used a precomputed `numBlocks * BLOCK_BYTES` which implicitly included overflow space, but the single-leaf path skipped it entirely. The result was that `serializedSize()` reported a value smaller than the bytes actually written, causing buffer underflows or data corruption when the serialized form was later read.

### Before / After

```java
// BEFORE – ignores overflow collision bytes for single leaf
if (numBlocks == 1)
    return (BLOCK_HEADER_BYTES + ((int) tokenCount * 16));
```

```java
// AFTER – adds overflow collision bytes when present
if (numBlocks == 1)
    return BLOCK_HEADER_BYTES +
           ((int) tokenCount * BLOCK_ENTRY_BYTES) +
           (((Leaf) root).overflowCollisionCount() * OVERFLOW_ENTRY_BYTES);
```

---

## How to Spot

Whenever a serialization size calculation has separate fast-path and slow-path branches, verify that every component written by `serialize()` is also counted in every branch of `serializedSize()`. A useful mechanical check is to assert `serializedSize() == bytesWritten` in a unit test covering both small and large inputs, especially inputs that trigger the overflow collision path.

---

## Test

`TokenTreeTest.testSerializedSizeDynamic/Static` is extended to run against both `simpleTokenMap` (single leaf, no collisions) and `bigTokensMap` (multi-block), asserting that the size returned equals the number of bytes actually written to disk.
