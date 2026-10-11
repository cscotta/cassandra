# Bug 0012: Vectorized Arrow reader silently misreads unsigned Parquet integers as signed

**Commit**: 9bd214e5063cccb41dbc724462ad0a3f8fed46f2 · **Ticket**: #14547

## Detection Rule

When a reader allocates a typed vector (e.g. `IntVector`, `BigIntVector`) based on a Parquet logical type annotation, verify that unsigned integer annotations are rejected or explicitly handled before allocation — not assumed to be compatible with the signed Java type.

## Code Smell

A `visitInt` or equivalent logical-type visitor method that calls `vector.allocateNew(batchSize)` and returns a `ReadType` without first checking `intLogicalType.isSigned()` — the signedness flag is present in the API but never consulted before mapping to a signed container type.

## Tags

- scope: universal
- category: wrong-filter-result
- code-pattern: unsigned-read-as-signed
- severity: silent-wrong-result
- subsystem: arrow / vectorized-reader / parquet

## Root Cause

The vectorized `VectorizedArrowReader` visits Parquet `INT` logical type annotations and allocates `IntVector` (32-bit signed) or `BigIntVector` (64-bit signed) regardless of the `isSigned()` flag. Values above `Integer.MAX_VALUE` (UINT32) or `Long.MAX_VALUE` (UINT64) wrap to negative numbers silently. The non-vectorized `BaseParquetReaders` path already rejects `uint64` with `UnsupportedOperationException`, but the vectorized path had no such guard, creating an inconsistency and data corruption for any caller using the Arrow reader on unsigned-integer Parquet files.

## Example

```java
// BEFORE — no signedness check; UINT32 silently becomes negative signed int
if (bitWidth == 8 || bitWidth == 16 || bitWidth == 32) {
    ((IntVector) vector).allocateNew(batchSize);
    return Optional.of(
        new LogicalTypeVisitorResult(vector, ReadType.INT, (int) IntVector.TYPE_WIDTH));
} else if (bitWidth == 64) {
    ((BigIntVector) vector).allocateNew(batchSize);
    return Optional.of(
        new LogicalTypeVisitorResult(vector, ReadType.LONG, (int) BigIntVector.TYPE_WIDTH));
}
```

```java
// AFTER — reject UINT32 and UINT64 before allocating signed containers
if (bitWidth == 8 || bitWidth == 16 || bitWidth == 32) {
    Preconditions.checkArgument(
        intLogicalType.isSigned() || bitWidth < 32, "Cannot read UINT32 as an int value");
    ((IntVector) vector).allocateNew(batchSize);
    return Optional.of(
        new LogicalTypeVisitorResult(vector, ReadType.INT, (int) IntVector.TYPE_WIDTH));
} else if (bitWidth == 64) {
    Preconditions.checkArgument(
        intLogicalType.isSigned(), "Cannot read UINT64 as a long value");
    ((BigIntVector) vector).allocateNew(batchSize);
    return Optional.of(
        new LogicalTypeVisitorResult(vector, ReadType.LONG, (int) BigIntVector.TYPE_WIDTH));
}
```
