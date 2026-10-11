# Bug 1387: Ensure memtable flush cannot expire commit log entries from its future

**Commit**: 7e3f6151ab · **Ticket**: CASSANDRA-8383
**Subsystem**: db/Memtable.java, db/ColumnFamilyStore.java, db/DataTracker.java, db/commitlog/CommitLog.java
**Category**: Race condition / missing synchronization

---

## Root Cause

When a memtable flush was initiated, the code sampled a `minReplayPosition` from the commit log before issuing the write barrier. Writes that arrived between sampling the position and the barrier being issued could receive a commit log position *greater* than the sampled minimum, yet still end up in the being-flushed memtable. After the flush completed the commit log would discard everything up to the recorded position, even though some of those "future" writes had never been durably flushed to disk. A node crash after log truncation but before an SSTable flush committed those rows would silently lose data.

The fix converts `lastReplayPosition` from a simple field into a cooperatively maintained `AtomicReference`. Both incoming writes and the flush initiator race to raise the value via CAS; the flush initiator then wraps the final value in a sentinel `LastReplayPosition` subclass which signals "boundary finalized". Only after the fence can the commit log safely truncate up to that boundary.

### Before / After

```java
// BEFORE — replay position sampled before write barrier issued;
//          new writes could exceed this bound and still land in the flushing memtable
final ReplayPosition minReplayPosition = CommitLog.instance.getContext();
for (ColumnFamilyStore cfs : concatWithIndexes())
{
    Memtable mt = cfs.data.switchMemtable(truncate);
    mt.setDiscarding(writeBarrier, minReplayPosition);
    ...
}
writeBarrier.issue();
```

```java
// AFTER — position determined cooperatively after barrier is issued
AtomicReference<ReplayPosition> lastReplayPositionHolder = new AtomicReference<>();
for (ColumnFamilyStore cfs : concatWithIndexes())
{
    Memtable mt = cfs.data.switchMemtable(truncate);
    mt.setDiscarding(writeBarrier, lastReplayPositionHolder);
    ...
}
// CAS loop raises to max seen position, then stamps it as final
ReplayPosition lastReplayPosition;
while (true)
{
    lastReplayPosition = new Memtable.LastReplayPosition(CommitLog.instance.getContext());
    ReplayPosition current = lastReplayPositionHolder.get();
    if ((current == null || current.compareTo(lastReplayPosition) <= 0)
        && lastReplayPositionHolder.compareAndSet(current, lastReplayPosition))
        break;
}
writeBarrier.issue();
```

---

## How to Spot

Any code that samples a monotonic counter or log position *before* a memory fence or barrier and then uses that sample to bound what gets discarded is at risk. The sampling and the barrier must be ordered so that the sample is taken after all operations that may be counted have been observed; a cooperative CAS protocol is one correct approach when producers are concurrent.

---

## Test

No test added.
