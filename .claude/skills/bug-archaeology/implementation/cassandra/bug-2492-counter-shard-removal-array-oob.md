# Bug 2492: ArrayIndexOutOfBoundsException in counter shard removal — header size miscalculated

**Commit**: 6d183c1a52 · **Ticket**: CASSANDRA-3514

## Detection Rule

When computing the new serialized size after removing entries from a structure that has both a fixed-size body entry and a variable-size header entry per shard type, using the same removal count for both sizes produces an incorrect buffer size when only a subset of removed entries have header records.

## Code Smell

`int removedHeaderSize = removedShards * HEADER_ELT_LENGTH` where `removedShards` counts all removed shards but only delta-type shards have header entries — non-delta shards should not contribute to `removedHeaderSize`.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: wrong-removal-count-for-variable-header
- severity: crash
- subsystem: storage-engine

## Root Cause

In `CounterContext.removeOldShards()`, the header has an entry only for delta-type shards, while the body has an entry for every shard. The code used the same counter `removedShards` for both `removedHeaderSize = removedShards * HEADER_ELT_LENGTH` and `removedBodySize = removedShards * STEP_LENGTH`. When non-delta (global) shards were removed, the calculated `removedHeaderSize` was too large, making `newSize` too small, causing an `ArrayIndexOutOfBoundsException` when writing into the undersized buffer.

## Example

```java
// BEFORE
int removedHeaderSize = removedShards * HEADER_ELT_LENGTH;  // wrong: counts all shards
int newSize = context.remaining() - removedHeaderSize - (removedShards * STEP_LENGTH);
```

```java
// AFTER
int removedHeaderSize = removedDelta * HEADER_ELT_LENGTH;  // correct: only delta shards have headers
int removedBodySize = removedShards * STEP_LENGTH;
int newSize = context.remaining() - removedHeaderSize - removedBodySize;
```
