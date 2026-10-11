# Bug 1683: Fix SSTable Not Released If Stream Session Fails

**Commit**: 04fd84c3d5 · **Ticket**: CASSANDRA-6818
**Subsystem**: src/java/org/apache/cassandra/streaming
**Category**: Resource leak

---

## Root Cause

`SSTableReader.releaseReference()` was called unconditionally at the end of `StreamWriter.write()` and `CompressedStreamWriter.write()`, meaning the reference was released only on the successful write path. If a stream session failed before the writer completed, or if an ACK was never received after the file was sent, the `SSTableReader` reference held by the outgoing transfer task was never released. Additionally, on session failure only `StreamReceiveTask.abort()` was called for receivers; the outgoing `StreamTransferTask` had no `abort()` path at all.

The fix moves reference release out of the writers and into `StreamTransferTask.complete()` (on success) and the new `StreamTransferTask.abort()` method (on failure). Session failure now aborts both receivers and transfers uniformly via the shared `StreamTask.abort()` contract. A scheduled timeout also ensures the reference is eventually freed if an ACK is never received.

### Before / After

```java
// BEFORE: release inside StreamWriter, only on normal completion
sstable.releaseReference();  // at end of write()
```

```java
// AFTER: release owned by StreamTransferTask on complete or abort
public void complete(int sequenceNumber)
{
    OutgoingFileMessage file = files.remove(sequenceNumber);
    if (file != null)
    {
        file.sstable.releaseReference();
        if (files.isEmpty())
        {
            timeoutExecutor.shutdownNow();
            session.taskCompleted(this);
        }
    }
}

public void abort()
{
    for (OutgoingFileMessage file : files.values())
        file.sstable.releaseReference();
    timeoutExecutor.shutdownNow();
}
```

```java
// BEFORE: only receivers aborted on session failure
for (StreamReceiveTask srt : receivers.values())
    srt.abort();

// AFTER: both receivers and transfers aborted
for (StreamTask task : Iterables.concat(receivers.values(), transfers.values()))
    task.abort();
```

---

## How to Spot

Any resource acquired at task-start should be released in all exit paths: normal completion, error, and cancellation. When `releaseReference` (or any analogous cleanup) lives only inside a success-path method, failures silently leak. Review code where a try/finally or explicit abort path is absent from transfer or producer tasks that hold external references.

---

## Test

`StreamTransferTaskTest.testScheduleTimeout()` was added. It creates two SSTables, schedules timeouts, and verifies that completing a file cancels its timeout, that a fired timeout advances the session state, and that scheduling a timeout after all files are done returns null.
