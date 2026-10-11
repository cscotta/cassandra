# Bug 2178: Truncate Flushes All Column Families to Advance Commit Log Instead of Persisting Replay Position

**Commit**: 38bfc6dca0 · **Ticket**: CASSANDRA-4906
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java, src/java/org/apache/cassandra/db/SystemTable.java, src/java/org/apache/cassandra/db/commitlog/CommitLogReplayer.java, src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: State not cleaned up

---

## Root Cause

The previous truncation implementation forced a flush of every column family on the node in order to advance the commit log replay position past the truncation point. This was necessary because the replay position was only tracked through SSTable metadata: without flushing all CFs, any pre-truncate commit log segments would be replayed on restart (since other CFs still referenced those segments). The fix instead persists the truncation replay position directly in the `system.local` table as a `map<uuid, blob>` entry keyed by CF UUID. On restart, `CommitLogReplayer` reads these stored positions and advances the per-CF replay position to be at least the truncation point, eliminating the need to flush every other CF.

### Before / After

```java
// BEFORE (truncate had to flush all CFs to allow commit log segment discard)
CommitLog.instance.forceNewSegment();
for (ColumnFamilyStore cfs : ColumnFamilyStore.all())
    cfs.forceFlush();
waitForActiveFlushes();
CommitLog.instance.discardCompletedSegments(metadata.cfId, position.get());
```

```java
// AFTER (save replay position to system table; replayer checks it on restart)
ReplayPosition replayAfter = main.discardSSTables(truncatedAt);
SystemTable.saveTruncationPosition(main, replayAfter);
```

---

## How to Spot

When a correctness invariant requires coordinating state across many independent components (e.g., "flush everything to advance a shared log pointer"), consider whether the invariant can be maintained by persisting a small piece of metadata instead. Cluster-wide forced flushes are both expensive and brittle.

---

## Test

No test added.
