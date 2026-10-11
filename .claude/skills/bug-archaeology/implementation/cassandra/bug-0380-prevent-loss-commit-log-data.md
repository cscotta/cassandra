# Bug 0380: Commit Log Data Loss When Moving SSTables Between Nodes

**Commit**: d84c6e9810 · **Ticket**: CASSANDRA-16619
**Subsystem**: src/java/org/apache/cassandra/db/commitlog, src/java/org/apache/cassandra/io/sstable/metadata
**Category**: Wrong serialization / deserialization

---

## Root Cause

`CommitLogReplayer.persistedIntervals()` built a replay-exclusion set from the `commitLogIntervals` of every on-disk SSTable. When SSTables were streamed from another node, their embedded `commitLogIntervals` referred to commit log positions on the originating node, not the local node. Including those intervals in the local replay filter caused the replayer to skip replaying real local mutations, resulting in data loss. The fix adds an `originatingHostId` field to SSTable stats metadata; the replayer now only includes intervals from SSTables that were written on the local node.

### Before / After

```java
// BEFORE – all SSTables contribute intervals regardless of origin
for (SSTableReader reader : onDisk)
    builder.addAll(reader.getSSTableMetadata().commitLogIntervals);
```

```java
// AFTER – only local-origin SSTables contribute intervals
for (SSTableReader reader : onDisk)
{
    UUID originatingHostId = reader.getSSTableMetadata().originatingHostId;
    if (originatingHostId != null && originatingHostId.equals(localhostId))
        builder.addAll(reader.getSSTableMetadata().commitLogIntervals);
    else
        skippedSSTables.add(reader.getFilename());
}
```

---

## How to Spot

Any component that uses SSTable metadata to gate or skip operations (replay, truncation, repair) must account for the fact that SSTables can be streamed in from other nodes and therefore carry metadata that is only valid in the context of their originating node. Validate the provenance of each metadata field before trusting it for local decision-making.

---

## Test

`MetadataSerializerTest` was extended with round-trip assertions for `originatingHostId`; `CommitLogTest` and `RecoveryManagerFlushedTest` received additional checks for the replay-filter path.
