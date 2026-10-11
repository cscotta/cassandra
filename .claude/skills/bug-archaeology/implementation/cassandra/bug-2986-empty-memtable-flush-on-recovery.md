# Bug 2986: Empty memtable flushed to SSTable on recovery, causing SSTable.close() error

**Commit**: 85c7c39ad1 · **Ticket**: n/a

## Detection Rule
When flushing a memtable during recovery, check whether it is clean before invoking the flush so that empty SSTables are never created.

## Code Smell
`flushOnRecovery()` unconditionally calls `flush(CommitLog.CommitLogContext.NULL)` without checking whether the memtable has any data; `SSTable.close()` errors when the SSTable is empty.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: unconditional-flush-empty
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
During commit-log recovery, `flushOnRecovery()` was called for every column family, including those with no dirty data. Flushing an empty memtable produces an empty SSTable, and `SSTable.close()` throws an error when it encounters an empty file. Additionally, empty SSTables create unnecessary compaction work. The fix guards the flush with `isClean()`.

## Example
```java
// BEFORE: always flushes, crashes on empty SSTable
void flushOnRecovery() throws IOException {
    flush(CommitLog.CommitLogContext.NULL);
}

// AFTER: skip flush when memtable has no data
void flushOnRecovery() throws IOException {
    if (!isClean())
        flush(CommitLog.CommitLogContext.NULL);
}
```
