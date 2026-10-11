# Bug 2739: AbstractCommutativeType.compose reads raw long bytes instead of counter context total

**Commit**: 653dcc63e0 · **Ticket**: n/a

## Detection Rule
When a type's on-disk representation is a structured context rather than a plain scalar, `compose()` must delegate to the context reader, not to a raw byte-buffer primitive reader.

## Code Smell
`AbstractCommutativeType.compose(ByteBuffer)` calls `ByteBufferUtil.toLong(bytes)`, treating the counter context blob as a raw 8-byte long, which gives garbage values because the context encodes per-node shard counts, not a bare long.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: raw-read-of-structured-context
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
Counter columns store a `CounterContext` — a per-replica shard structure — not a single long. `ByteBufferUtil.toLong()` reads the first 8 bytes as a big-endian long, which is actually the node-count / shard metadata of the context, not the logical counter total. The fix replaces the call with `CounterContext.instance().total(bytes)`, which correctly sums all replica shards to produce the logical counter value.

## Example
```java
// BEFORE — reads raw bytes as a long (wrong for counter context)
public Long compose(ByteBuffer bytes) {
    return ByteBufferUtil.toLong(bytes);
}
```
```java
// AFTER — delegates to CounterContext which sums shards
public Long compose(ByteBuffer bytes) {
    return CounterContext.instance().total(bytes);
}
```
