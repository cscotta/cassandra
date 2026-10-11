## Commit

- Hash: cf7029c026
- Tickets: KAFKA-13093
- Subject: KAFKA-13093: Always use CURRENT_MAGIC_VALUE in MemoryRecords.filterTo for log compaction

## Subsystem

clients / common / record / MemoryRecords

## Category

Wrong constant / default value

## Root cause

`MemoryRecords.filterTo` (used during log compaction) preserved the original record batch magic version (v0, v1, or v2). During compaction of very old segments, retained records would be written back as v0/v1, preventing the log from ever upgrading to v2 format even after the broker's `message.format.version` was set to 2.x. Compacted topics remained perpetually in the old format, wasting space (v0/v1 lack record-level headers, compression improvements) and complicating future upgrades. The fix is to always rewrite retained records using `CURRENT_MAGIC_VALUE` (v2).

## Before

```java
// Magic version preserved from source batch
outputBuf = MemoryRecordsBuilder(sourceMagic, ...);
```

## After

```java
// Always upgrade to current magic during compaction
outputBuf = MemoryRecordsBuilder(RecordBatch.CURRENT_MAGIC_VALUE, ...);
```

Also: `filterTo` signature simplified — `TopicPartition` and `maxRecordBatchSize` parameters removed as they were unused after the magic-version change.

## How to spot

Log compaction paths that copy `sourceBatch.magic()` into the output builder — they will forever perpetuate legacy record formats even when the cluster is configured for a newer one.

## Test

`MemoryRecordsTest` verifies that v0 and v1 records are upgraded to v2 during `filterTo` compaction.
