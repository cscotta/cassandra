# Bug 1443: Fix High Memory Use Due to Tracking Reads on Incrementally Opened SSTables

**Commit**: c5cbd58a3b · **Ticket**: CASSANDRA-8066
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Resource leak

---

## Root Cause

`SSTableReader` schedules a periodic background task (`readMeterSyncFuture`) to persist per-SSTable read-rate metrics to disk. For "early-opened" SSTable readers (created during incremental compaction to serve live reads before the file is fully written), this tracking was incorrectly enabled because the constructor only checked for system keyspace and client mode. Each early-open created a new `ScheduledFuture` that queued unbounded work onto the executor; because early-open readers are created and replaced frequently during compaction, the executor queue grew without bound, consuming significant heap. The fix introduces a typed `OpenReason` enum and skips read-meter initialization for any non-`NORMAL` open reason.

### Before / After

```java
// BEFORE
if (Keyspace.SYSTEM_KS.equals(desc.ksname) || Config.isClientMode())
{
    readMeter = null;
    readMeterSyncFuture = null;
}
```

```java
// AFTER
if (Keyspace.SYSTEM_KS.equals(desc.ksname) || Config.isClientMode() || openReason != OpenReason.NORMAL)
{
    readMeter = null;
    readMeterSyncFuture = null;
}
```

---

## How to Spot

Whenever a class that starts a background periodic task (timer, scheduled executor, etc.) can be instantiated in multiple contexts — normal operation vs. temporary/incremental variants — there must be an explicit guard ensuring the background task is only started for the canonical lifetime case. A boolean flag like the old `isOpenEarly` is also a code smell: replacing it with an enum forces callers to consider all cases and makes it easier to add new open modes safely.

---

## Test

No test added.
