# Bug 0536: Bad Merge Reverted FS Error Handling in Executor Service

**Commit**: a4af4aa60a · **Ticket**: CASSANDRA-15289
**Subsystem**: `src/java/org/apache/cassandra/concurrent/AbstractLocalAwareExecutorService.java`
**Category**: Wrong constant / default value

---

## Root Cause

A bad merge accidentally reverted the fix from CASSANDRA-14993, which had changed `AbstractLocalAwareExecutorService` to properly dispatch `CorruptSSTableException` and `FSError` through their dedicated handlers (`FileUtils.handleCorruptSSTable` and `FileUtils.handleFSError`) rather than the generic `JVMStabilityInspector.inspectThrowable`. The reversion also swapped back the import of tracing classes in place of the needed FS error utilities, and downgraded the log level from `error` back to `warn`. Without the specialised handlers, file-system errors would not trigger the configured disk-failure policy, potentially leaving a node silently serving data from a corrupt SSTable.

### Before / After

```java
// BEFORE (reverted state)
import org.apache.cassandra.tracing.TraceState;
import org.apache.cassandra.tracing.Tracing;
// ...
JVMStabilityInspector.inspectThrowable(t);
logger.warn("Uncaught exception on thread {}", Thread.currentThread(), t);
```

```java
// AFTER (restored fix)
import org.apache.cassandra.io.FSError;
import org.apache.cassandra.io.sstable.CorruptSSTableException;
import org.apache.cassandra.io.util.FileUtils;
// ...
logger.error("Uncaught exception on thread {}", Thread.currentThread(), t);
if (t instanceof CorruptSSTableException)
    FileUtils.handleCorruptSSTable((CorruptSSTableException) t);
else if (t instanceof FSError)
    FileUtils.handleFSError((FSError) t);
else
    JVMStabilityInspector.inspectThrowable(t);
```

---

## How to Spot

When reviewing merge commits that touch infrastructure-level exception handlers, verify that the set of imports in the merged result matches the logic: if the catch block dispatches to type-specific handlers the corresponding import must be present, and if only a generic inspector is called there should be no dead imports for specialised error types. A mismatch between imports and the catch body is a reliable signal that a merge silently clobbered an earlier fix.

---

## Test

No test added (merge fix restoring previously tested behaviour).
