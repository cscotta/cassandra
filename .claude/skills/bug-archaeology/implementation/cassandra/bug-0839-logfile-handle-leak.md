# Bug 0839: File Handle Leak When LogAwareFileLister Classifies Transaction Log Files

**Commit**: da07130e4e · **Ticket**: CASSANDRA-11594
**Subsystem**: SSTable lifecycle / LogAwareFileLister
**Category**: Resource leak

---

## Root Cause
`LogAwareFileLister.classifyFiles(File txnFile)` created a `LogFile` instance (which internally opens file-system directory descriptors via `LogReplica`) but never closed it. `LogFile` had a `close()` method and `LogReplica`/`LogReplicaSet` both held OS-level folder file descriptors (`NativeLibrary.tryOpenDirectory`), but none of these classes implemented `AutoCloseable`. When `classifyFiles` was called during snapshot listing, compaction, or schema changes — all of which enumerate transaction log files — one directory descriptor per log file was leaked. The fix makes `LogFile`, `LogReplica`, and `LogReplicaSet` all implement `AutoCloseable`, promotes their `close()` methods from package-private to `public`, and wraps the `LogFile` usages in `classifyFiles()` and `removeUnfinishedLeftovers()` with try-with-resources.

### Before / After
```java
// BEFORE — LogAwareFileLister.classifyFiles()
LogFile txn = LogFile.make(txnFile);
readTxnLog(txn);
classifyFiles(txn);
files.put(txnFile, FileType.TXN_LOG);
// txn never closed — directory descriptor leaked

// AFTER — LogAwareFileLister.classifyFiles()
try (LogFile txn = LogFile.make(txnFile)) {
    readTxnLog(txn);
    classifyFiles(txn);
    files.put(txnFile, FileType.TXN_LOG);
}

// BEFORE — LogFile / LogReplica / LogReplicaSet
final class LogFile { void close() { ... } }
final class LogReplica { void close() { ... } }

// AFTER
final class LogFile implements AutoCloseable { public void close() { ... } }
final class LogReplica implements AutoCloseable { public void close() { ... } }
public class LogReplicaSet implements AutoCloseable { public void close() { ... } }
```

---

## How to Spot
Any class that wraps an OS resource (file descriptor, socket, channel) and exposes a `close()` method without implementing `AutoCloseable` is a resource-leak hazard. Search for classes that call `NativeLibrary.tryOpenDirectory` or similar low-level open calls and verify that every call-site either uses try-with-resources or calls `close()` in a `finally` block. The tell-tale sign here was that `removeUnfinishedLeftovers()` did use a `finally` block but `classifyFiles()` did not, so normal startup cleanup was safe while the more-frequent snapshot and compaction paths leaked.

---

## Test
No test added in this commit.
