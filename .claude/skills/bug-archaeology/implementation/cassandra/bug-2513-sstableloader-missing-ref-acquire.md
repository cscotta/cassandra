# Bug 2513: SSTableLoader calls transferSSTables without acquiring SSTableReader references

**Commit**: 5cf72b7c53 · **Ticket**: CASSANDRA-3438

## Detection Rule
When `StreamOut.transferSSTables` is called, verify that `SSTableReader.acquireReferences()` has been called beforehand, because the streaming code assumes references are held and will dereference them during transfer.

## Code Smell
`StreamOut.transferSSTables(session, sstables, ranges, ...)` called directly without a preceding `SSTableReader.acquireReferences(sstables)` — the streaming path holds and releases a reference per SSTable, so calling it without acquiring causes a negative reference count and the reader can be compacted-away while streaming is in progress.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: missing-reference-acquisition
- severity: crash
- subsystem: streaming

## Root Cause
`StreamOut.transferSSTables()` internally calls `releaseReferences()` on the SSTables it sends. The SSTableLoader did not call `acquireReferences()` before handing the list to `transferSSTables()`, so the reference count went negative on the first release. This allowed the compaction manager to delete the underlying files while they were being streamed to a remote node, causing IOExceptions or data corruption during bulk load.

## Example
```java
// BEFORE
StreamOutSession session = StreamOutSession.create(...);
StreamOut.transferSSTables(session, sstables, ranges, OperationType.BULK_LOAD);
```
```java
// AFTER
StreamOutSession session = StreamOutSession.create(...);
// transferSSTables assumes references have been acquired
SSTableReader.acquireReferences(sstables);
StreamOut.transferSSTables(session, sstables, ranges, OperationType.BULK_LOAD);
```
