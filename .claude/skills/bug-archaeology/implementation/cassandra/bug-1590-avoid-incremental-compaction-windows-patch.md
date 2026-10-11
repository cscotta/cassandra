# Bug 1590: Avoid Incremental Compaction on Windows

**Commit**: 61b13b0e6a · **Ticket**: CASSANDRA-7365
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableRewriter.java
**Category**: Logic error in condition

---

## Root Cause

`SSTableRewriter.maybeReopenEarly()` uses preemptive (incremental) SSTable opening to allow reads from partially-written SSTables during compaction. This relies on the ability to open a file while it is still being written to — a capability that works on Unix but is not supported on Windows because Windows locks files opened for writing against concurrent reads. Without the OS check, compaction on Windows would attempt to reopen SSTables mid-write and fail with an access error.

### Before / After

```java
// BEFORE — no OS guard
if (writer.getFilePointer() - currentlyOpenedEarlyAt > preemptiveOpenInterval)
```

```java
// AFTER — skip early reopen on Windows
if (FBUtilities.isUnix() && writer.getFilePointer() - currentlyOpenedEarlyAt > preemptiveOpenInterval)
```

---

## How to Spot

Any code that opens or memory-maps a file that another thread or process has open for writing should be guarded with an OS check. On Windows, file-sharing semantics differ from POSIX; concurrent open-for-read of a write-locked file will fail. Look for `getFilePointer`-triggered reopen patterns and confirm they are either Windows-safe or gated on `FBUtilities.isUnix()`.

---

## Test

No test added.
