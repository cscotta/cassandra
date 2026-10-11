# Bug 2314/2315: Leveled compaction SSTable size check uses in-memory logical position instead of on-disk compressed size

**Commit**: f422b98140 (1.1.x) / 2ce8274c60 (1.0.x) · **Ticket**: CASSANDRA-4341

## Detection Rule

When checking whether a compaction output file has exceeded a target size threshold, verify that the size measurement comes from the actual on-disk bytes written, not the uncompressed logical file pointer — especially when compression is enabled.

## Code Smell

A size threshold check like `position > sstableSizeInMB * 1024L * 1024L` where `position` is a logical in-memory pointer passed from `writer.getFilePointer()` rather than the compressed bytes actually flushed to disk.

## Tags

- scope: cassandra
- category: wrong-constant
- code-pattern: logical-vs-physical-file-size
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

`LeveledCompactionTask.newSSTableSegmentThresholdReached()` received the current logical file pointer as a parameter (`long position`) and compared it to the target size limit. When compression was enabled, logical position measured uncompressed bytes, which could be significantly larger than the on-disk compressed size. This caused LCS to split output SSTables too early, creating more (smaller) files than intended and degrading LCS performance. The fix adds a `getOnDiskFilePointer()` method to `SSTableWriter` (with a compressed override in `CompressedSequentialWriter`) and removes the `position` parameter, querying the writer directly.

## Example

```java
// BEFORE
protected boolean newSSTableSegmentThresholdReached(SSTableWriter writer, long position)
{
    return position > sstableSizeInMB * 1024L * 1024L;  // position is uncompressed logical offset
}
// called as:
if (!nni.hasNext() || newSSTableSegmentThresholdReached(writer, indexEntry.position))
```

```java
// AFTER
protected boolean newSSTableSegmentThresholdReached(SSTableWriter writer) throws IOException
{
    return writer.getOnDiskFilePointer() > sstableSizeInMB * 1024L * 1024L;  // actual bytes on disk
}
// CompressedSequentialWriter overrides getOnDiskFilePointer() to return compressed bytes
```
