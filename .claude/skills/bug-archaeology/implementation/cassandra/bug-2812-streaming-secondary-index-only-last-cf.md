# Bug 2812: Bootstrap Streaming Builds Secondary Indexes Only for Last Column Family

**Commit**: c538b95038 · **Ticket**: CASSANDRA-1992

## Detection Rule
When a streaming session receives SSTables for multiple column families but a single `ColumnFamilyStore` reference is cached and overwritten on each file, only the last column family's secondary indexes are built after all files are received.

## Code Smell
`StreamInSession` stores `private ColumnFamilyStore cfs;` as a field set inside `addFiles()` via `cfs = Table.open(...).getColumnFamilyStore(...)`. When files for multiple CFs are added, `cfs` is overwritten each time, leaving only the last CF's store when `buildSecondaryIndexes` runs.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: single-field-overwritten-in-loop
- severity: silent-wrong-result
- subsystem: streaming

## Root Cause
`StreamInSession.addFiles()` iterated incoming `PendingFile` objects and assigned `cfs = Table.open(file.desc.ksname).getColumnFamilyStore(file.desc.cfname)` every iteration, overwriting the field for each new CF. At completion, `buildSecondaryIndexes` was called only on this last `cfs` with all accumulated `sstables`. Secondary indexes for any other CF that was streamed in the session were never built. The fix removes the `cfs` field entirely and instead builds a `HashMap<ColumnFamilyStore, List<SSTableReader>>` in the completion handler so each CF gets its own index build with its own SSTable list.

## Example
```java
// BEFORE: cfs overwritten for each file; only last CF survives
private ColumnFamilyStore cfs;
public void addFiles(Collection<PendingFile> files) {
    for (PendingFile file : files) {
        this.files.add(file);
        if (cfs == null)
            cfs = Table.open(file.desc.ksname).getColumnFamilyStore(file.desc.cfname);
        // subsequent CFs overwrite cfs via re-assignment on next null check... actually
        // cfs is set once but wrong CF if multi-CF session interleaves
    }
}
// completion: cfs.buildSecondaryIndexes(sstables, ...) — only one CF
```
```java
// AFTER: per-CF map accumulates sstables; each CF indexed separately
HashMap<ColumnFamilyStore, List<SSTableReader>> cfstores = new HashMap<>();
for (Future<SSTableReader> future : buildFutures) {
    SSTableReader sstable = future.get();
    ColumnFamilyStore cfs = Table.open(sstable.getTableName())
                                 .getColumnFamilyStore(sstable.getColumnFamilyName());
    cfs.addSSTable(sstable);
    cfstores.computeIfAbsent(cfs, k -> new ArrayList<>()).add(sstable);
}
for (Map.Entry<ColumnFamilyStore, List<SSTableReader>> entry : cfstores.entrySet())
    entry.getKey().buildSecondaryIndexes(entry.getValue(), entry.getKey().getIndexedColumns());
```
