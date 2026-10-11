# Bug 2630: AntiEntropy streaming uses unreferenced SSTable collection that can be concurrently compacted away

**Commit**: 3ac0d33e31 · **Ticket**: n/a

## Detection Rule
When SSTable files are passed to a streaming session, always acquire a reference-counted snapshot via `markCurrentSSTablesReferenced()` rather than the raw `getSSTables()` collection, which can be mutated by concurrent compaction.

## Code Smell
Streaming code calls `cfstore.getSSTables()` and passes the result to `transferSSTables()`, but `getSSTables()` returns an unguarded snapshot; without reference counting, concurrent compaction can delete those files before the stream completes.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: unreferenced-sstable-streaming
- severity: crash
- subsystem: streaming

## Root Cause
A bad merge left `AntiEntropyService` calling `cfstore.getSSTables()` where it should have called `cfstore.markCurrentSSTablesReferenced()`. The former returns a plain collection that is not protected from compaction; the latter atomically marks each SSTable referenced so they cannot be deleted during the stream. Using the unprotected collection could cause the streamer to read or open SSTable files that had already been compacted away and deleted.

## Example
```java
// BEFORE
Collection<SSTableReader> sstables = cfstore.getSSTables();

// AFTER
// We acquire references for transferSSTables
Collection<SSTableReader> sstables = cfstore.markCurrentSSTablesReferenced();
```
