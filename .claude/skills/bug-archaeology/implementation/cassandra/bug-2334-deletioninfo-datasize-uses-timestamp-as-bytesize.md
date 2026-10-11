# Bug 2334: DeletionInfo.dataSize() adds raw tombstone timestamp value instead of serialized byte size

**Commit**: 5ab69b62cc · **Ticket**: n/a

## Detection Rule

When computing the serialized byte size of a collection of tombstone/deletion records, verify that each field's size contribution is measured in bytes (e.g., via `TypeSizes.NATIVE.sizeof(...)`) rather than using the raw field value directly.

## Code Smell

`size += r.data.markedForDeleteAt` inside a size-computation loop — this adds the raw `long` deletion timestamp as if it were a byte count, producing wildly incorrect sizes.

## Tags

- scope: java
- category: wrong-constant
- code-pattern: value-used-as-byte-size
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`DeletionInfo.dataSize()` iterated over range tombstones and accumulated their sizes. For each tombstone, it wrote `size += r.data.markedForDeleteAt` — using the deletion timestamp (a `long` in microseconds, e.g., `1340000000000000`) as if it were a number of bytes. The correct computation is to add the serialized sizes of the tombstone's `min` and `max` ByteBuffer bounds plus the size of the `markedForDeleteAt` long field via `TypeSizes.NATIVE.sizeof(...)`.

## Example

```java
// BEFORE
for (RangeTombstone r : ranges)
    size += r.data.markedForDeleteAt;  // BUG: timestamp value, not byte size!
```

```java
// AFTER
for (RangeTombstone r : ranges)
{
    size += r.min.remaining() + r.max.remaining();
    size += TypeSizes.NATIVE.sizeof(r.data.markedForDeleteAt);  // sizeof(long) = 8
}
```
