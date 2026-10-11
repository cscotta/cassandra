# Bug 2867: SSTable stat histograms could be null after construction

**Commit**: 48c6d5b3d8 · **Ticket**: CASSANDRA-1620

## Detection Rule
When a constructor chain passes null for fields that are later used unconditionally, check that every code path that creates objects through that chain provides non-null defaults.

## Code Smell
`SSTableReader` was constructed with `null, null` for `rowsize` and `columncount` parameters, and then the constructor assigned those nulls to `estimatedRowSize`/`estimatedColumnCount`; any subsequent call to `getEstimatedRowSize()` or `getEstimatedColumnCount()` would NPE.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-passed-to-final-field
- severity: crash
- subsystem: storage-engine

## Root Cause
The `SSTableReader.open` path that loaded statistics called a separate `loadStatistics` method after construction, but the constructor set the fields to null in the meantime. Statistics loading was folded into `open` before construction so that histograms are always non-null, and a new constructor overload accepts pre-built histograms explicitly.

## Example
```java
// BEFORE
SSTableReader sstable = new SSTableReader(descriptor, ..., null, null);
// estimatedRowSize == null here
sstable.loadStatistics(descriptor); // too late — field already null
```
```java
// AFTER
EstimatedHistogram rowSizes = statsFile.exists()
    ? deserialize(dis) : SSTable.defaultRowHistogram();
SSTableReader sstable = new SSTableReader(descriptor, ..., rowSizes, columnCounts);
// estimatedRowSize always non-null
```
