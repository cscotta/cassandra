# Bug 0489: Streaming Stats Undercount Files During Entire-SSTable Streaming

**Commit**: b1411a4318 · **Ticket**: CASSANDRA-15694
**Subsystem**: src/java/org/apache/cassandra/streaming/
**Category**: Wrong constant / default value

---

## Root Cause

When an entire SSTable is streamed (as opposed to partial/compressed streaming), an SSTable consists of multiple component files (Data, Index, Summary, Filter, etc.). The streaming stats code treated each `OutgoingStream` / `IncomingStream` as exactly one file regardless of mode, so for entire-SSTable transfers the file count was reported as 1 instead of the true component count. The `getTotalNumberOfFiles()` method on `StreamTransferTask` returned `streams.size()` — the number of SSTable objects — rather than the sum of component files per stream.

### Before / After

```java
// BEFORE
public synchronized int getTotalNumberOfFiles() {
    return streams.size();
}
// remoteStreamsReceived++; in StreamReceiveTask
```

```java
// AFTER
private int totalFiles = 0;

public void addTransferStream(OutgoingStream stream) {
    totalFiles += message.stream.getNumFiles();
}
public synchronized int getTotalNumberOfFiles() {
    return totalFiles;
}
// remoteStreamsReceived += stream.getNumFiles(); in StreamReceiveTask
```

---

## How to Spot

When an operation mode introduces a "batch" of underlying resources where the API previously assumed one resource per item, all accumulators that previously incremented by 1 need to be audited and updated to increment by the resource count. Look for counters that increment with `++` or `+= 1` in streaming/transfer code when an entire-file mode was added.

---

## Test

`EntireSSTableStreamingCorrectFilesCountTest` streams a real SSTable in entire-SSTable mode and asserts that `getTotalNumberOfFiles()` equals the manifest component count.
