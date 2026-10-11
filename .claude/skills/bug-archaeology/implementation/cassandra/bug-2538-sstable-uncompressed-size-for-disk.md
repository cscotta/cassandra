# Bug 2538: SSTable size calculations use uncompressed data length instead of on-disk length

**Commit**: d790ed1bc8 · **Ticket**: CASSANDRA-3338

## Detection Rule
When choosing a data directory for a new compaction output file or computing compaction size metrics, always use the on-disk (compressed) file size rather than the logical data size; for compressed SSTables these can differ by an order of magnitude.

## Code Smell
`sstable.length()` used for disk-space checks in `getDataFileLocation()`, compaction size logging, and size-tiered bucket computation — for compressed SSTables `length()` returns the uncompressed logical size, so disk-full checks vastly overestimate available space needed and size-tiered groupings bucket the wrong SSTables together.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: uncompressed-size-for-disk-check
- severity: performance
- subsystem: compaction

## Root Cause
`SSTableReader.length()` returned `dfile.length`, which is the uncompressed data length. For compressed SSTables the on-disk footprint is the compressed file size, exposed via a new `onDiskLength()` method. All places that needed disk-space information (choosing a data directory, estimating compaction output size, size-tiered bucketing, and range-read byte counting) were using the wrong method, causing incorrect compaction scheduling and potential "disk full" errors when the real file would fit.

## Example
```java
// BEFORE — uncompressed size used for disk checks
long size = sstable.length();          // returns logical data length
String location = cfs.table.getDataFileLocation(size);
```
```java
// AFTER — on-disk size used for disk checks
long size = sstable.onDiskLength();    // returns compressed file size
String location = cfs.table.getDataFileLocation(size);
```
