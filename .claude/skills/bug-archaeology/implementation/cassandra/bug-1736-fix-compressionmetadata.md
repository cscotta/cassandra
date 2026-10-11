# Bug 1736: Fix resetAndTruncate:ing CompressionMetadata

**Commit**: 64098f7d6f · **Ticket**: CASSANDRA-6791
**Subsystem**: src/java/org/apache/cassandra/io/compress
**Category**: Off-by-one

---

## Root Cause

When `CompressedSequentialWriter.resetAndTruncate` is called to roll back to a previously marked position, it passed `realMark.nextChunkIndex` directly to `metadataWriter.resetAndTruncate`. The field name `nextChunkIndex` already represents the index of the *next* chunk to be written after the mark point — so passing it directly caused the metadata writer to retain one extra chunk entry beyond the mark, corrupting the compression metadata index.

### Before / After

```java
// BEFORE
metadataWriter.resetAndTruncate(realMark.nextChunkIndex);
```

```java
// AFTER
metadataWriter.resetAndTruncate(realMark.nextChunkIndex - 1);
```

---

## How to Spot

When a field is named `nextFoo` (carrying the semantic of "the next index after the current one"), passing it as an inclusive truncation point is an off-by-one. Review every call site where a `next*` or `end*` index is used as an argument to truncation, reset, or slice operations that expect an inclusive last-element index.

---

## Test

Writes 20 bytes, marks, writes 40 more bytes (spanning multiple compression chunks), resets to the mark, writes 20 more bytes, then reads back the full content and asserts it is exactly 40 `x` characters with no `y` garbage.
