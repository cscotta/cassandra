# Bug 2815: SSTableWriter Writes rowSizes Histogram Twice Instead of columnCounts

**Commit**: 3bb7b70a72 · **Ticket**: CASSANDRA-1976

## Detection Rule
When two `serialize` calls are made sequentially to write two different histograms to a stats file, and both calls reference the same variable instead of separate variables, the second histogram is never persisted and is replaced by a duplicate of the first.

## Code Smell
Two consecutive `EstimatedHistogram.serializer.serialize(rowSizes, out)` calls — the second one should reference `columnnCounts` (note the typo in the field name as well).

## Tags
- scope: java
- category: logic-error
- code-pattern: copy-paste-wrong-variable
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`SSTableWriter.writeStatistics()` serializes two `EstimatedHistogram` objects to the `.db-Statistics.db` file: row sizes and column counts. Due to a copy-paste error, both `serialize` calls passed `rowSizes` — meaning the column count histogram was never written, and the file contained the row sizes histogram twice. Readers of this stats file would silently read the wrong histogram as column counts, affecting compaction decisions and statistics display. The fix changes the second call to `columnnCounts`.

## Example
```java
// BEFORE: rowSizes written twice, columnnCounts never written
EstimatedHistogram.serializer.serialize(rowSizes, out);
EstimatedHistogram.serializer.serialize(rowSizes, out);  // copy-paste bug
```
```java
// AFTER: each histogram written once
EstimatedHistogram.serializer.serialize(rowSizes, out);
EstimatedHistogram.serializer.serialize(columnnCounts, out);
```
