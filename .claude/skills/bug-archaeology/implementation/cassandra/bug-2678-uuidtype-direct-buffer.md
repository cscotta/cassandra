# Bug 2678: UUIDType comparison fails on direct ByteBuffers

**Commit**: db69b38911 · **Ticket**: CASSANDRA-2682

## Detection Rule
When implementing a byte-level comparator for a `ByteBuffer`, access individual bytes via `ByteBuffer.get(index)` rather than extracting the backing array with `ByteBuffer.array()` and using array-offset arithmetic, which fails for direct (off-heap) buffers.

## Code Smell
Code like `int s1 = b1.arrayOffset() + b1.position(); byte[] o1 = b1.array(); int v1 = (o1[s1 + 6] >> 4) & 0x0f;` will throw `UnsupportedOperationException` when `b1` is a direct buffer.

## Tags
- scope: java
- category: wrong-serialization
- code-pattern: direct-buffer-array-access
- severity: crash
- subsystem: cql-query

## Root Cause
`UUIDType.compare()` and `compareTimestampBytes()` extracted the backing `byte[]` from each `ByteBuffer` using `b.array()` and combined it with `arrayOffset()` for indexed access. Direct buffers have no backing array, so this throws `UnsupportedOperationException`. The fix eliminates array extraction entirely and uses `ByteBuffer.get(position + offset)` for all byte reads.

## Example
```java
// BEFORE
int s1 = b1.arrayOffset() + b1.position();
byte[] o1 = b1.array();
int v1 = (o1[s1 + 6] >> 4) & 0x0f;
```
```java
// AFTER
int s1 = b1.position();
int v1 = (b1.get(s1 + 6) >> 4) & 0x0f;
```
