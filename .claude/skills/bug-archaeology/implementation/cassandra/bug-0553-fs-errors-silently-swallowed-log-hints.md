# Bug 0553: FS Errors Silently Swallowed in LogTransaction and Hints

**Commit**: ba32595546 · **Ticket**: CASSANDRA-15053
**Subsystem**: `src/java/org/apache/cassandra/db/lifecycle/`, `src/java/org/apache/cassandra/hints/`
**Category**: Logic error in condition

---

## Root Cause

Several call sites that invoked native file-system operations (`NativeLibrary.trySync`, `FileUtils.appendAndSync`, `SyncUtil.trySync`) caught or silently ignored `FSError` / `RuntimeException` rather than propagating them through the disk-failure policy handler. Additionally, `NativeLibrary.trySync` and `tryCloseFD` only logged fsync failures instead of throwing, and `LogReplica.create`/`open` silently accepted an invalid folder descriptor of `-1`. The cumulative effect was that disk errors during log-transaction writes or hints flushes were swallowed, leaving the node in an inconsistent state rather than triggering the configured disk-failure policy (stop/die).

### Before / After

```java
// BEFORE (NativeLibrary.trySync)
logger.warn("fsync({}) failed, errorno ({}) {}", fd, errno(e), e);

// BEFORE (LogReplica.create)
return new LogReplica(new File(fileName), NativeLibrary.tryOpenDirectory(folder.getPath()));
```

```java
// AFTER (NativeLibrary.trySync)
String errMsg = String.format("fsync(%s) failed, errno %s", fd, errno(e));
logger.warn(errMsg);
throw new FSWriteError(e, errMsg);

// AFTER (LogReplica.create)
int folderFD = NativeLibrary.tryOpenDirectory(folder.getPath());
if (folderFD == -1)
    throw new FSReadError(new IOException("Invalid folder descriptor ..."), folder.getPath());
return new LogReplica(new File(fileName), folderFD);
```

---

## How to Spot

Look for native I/O call sites that return sentinel values (e.g., `-1`) or can throw `FSError` where the caller only logs and continues. Any place that calls `trySync`, `appendAndSync`, or directory-open helpers without either checking the return value or catching and re-throwing via `FileUtils.handleFSErrorAndPropagate` is a candidate for this class of bug.

---

## Test

No test added.
