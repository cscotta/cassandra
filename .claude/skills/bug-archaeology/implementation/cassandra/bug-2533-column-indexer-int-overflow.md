# Bug 2533: ColumnIndexer uses int for file offsets, overflows for rows > 2 GB

**Commit**: e5c70deff8 · **Ticket**: CASSANDRA-3358

## Detection Rule
When tracking byte offsets within a data file that can exceed 2 GB, verify that accumulator variables are declared as `long`, not `int`; silent integer overflow produces negative or wrap-around offsets that corrupt the column index.

## Code Smell
`int endPosition = 0, startPosition = -1;` in `ColumnIndexer.serialize()` — as column sizes accumulate beyond 2,147,483,647 bytes, `endPosition` overflows to a negative value and the written `IndexInfo` offsets are wrong, corrupting reads for large rows.

## Tags
- scope: java
- category: off-by-one
- code-pattern: int-overflow-file-offset
- severity: data-loss
- subsystem: storage-engine

## Root Cause
`ColumnIndexer.serialize()` accumulated `endPosition` as an `int` while iterating columns to build the per-row index. Any row whose serialized column data exceeded 2 GB would cause `endPosition` to overflow, producing invalid `IndexHelper.IndexInfo` entries (with negative or wrap-around offsets). On read, the column lookup would seek to the wrong file position, returning garbage or throwing an `IOException`. The fix changes both `endPosition` and `startPosition` to `long`.

## Example
```java
// BEFORE
int endPosition = 0, startPosition = -1;  // overflows at ~2 GB
int indexSizeInBytes = 0;
for (IColumn column : columns) {
    endPosition += column.serializedSize();
    ...
}
```
```java
// AFTER
long endPosition = 0, startPosition = -1; // supports rows up to Long.MAX_VALUE
int indexSizeInBytes = 0;
for (IColumn column : columns) {
    endPosition += column.serializedSize();
    ...
}
```
