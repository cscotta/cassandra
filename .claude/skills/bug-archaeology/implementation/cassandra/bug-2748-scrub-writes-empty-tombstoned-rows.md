# Bug 2748: Scrubber Writes Empty Rows for Fully-Tombstoned Data

**Commit**: 0087c38a26 · **Ticket**: CASSANDRA-2296

## Detection Rule
When appending a compacted row during scrub, call `compactedRow.isEmpty()` before writing it; skip and count empty rows separately rather than writing zero-column rows to the output SSTable.

## Code Smell
`writer.append(getCompactedRow(...))` is called unconditionally; there is no check that the returned `AbstractCompactedRow` actually contains live columns before writing it.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: unconditional-write-of-empty-row
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause
After compaction merges a tombstoned row, all columns are dropped and the row becomes empty. The scrubber was passing this empty `AbstractCompactedRow` directly to `writer.append`, producing a corrupt zero-column row in the output SSTable. The fix checks `compactedRow.isEmpty()` and increments `emptyRows` rather than writing.

## Example
```java
// BEFORE
writer.append(getCompactedRow(row, cfs, sstable.descriptor, true));
goodRows++;
```
```java
// AFTER
AbstractCompactedRow compactedRow = getCompactedRow(row, cfs, sstable.descriptor, true);
if (compactedRow.isEmpty()) {
    emptyRows++;
} else {
    writer.append(compactedRow);
    goodRows++;
}
```
