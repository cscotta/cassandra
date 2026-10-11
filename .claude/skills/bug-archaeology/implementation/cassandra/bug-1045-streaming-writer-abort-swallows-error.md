# Bug 1045: Stream Retry Killed by Exception Thrown During SSTableWriter Abort

**Commit**: 068614ccc7 · **Ticket**: CASSANDRA-10557
**Subsystem**: Streaming
**Category**: Resource leak

---

## Root Cause
In both `StreamReader` and `CompressedStreamReader`, when an exception occurred during SSTable streaming the catch block called `writer.abort()` unconditionally. If `abort()` itself threw an exception, it replaced the original exception and propagated up, preventing the stream drain (`drain()`) from running and causing the entire stream retry to fail rather than cleanly recovering. Additionally, `createWriter()` was called before the try block, so an exception from writer creation would skip the drain entirely.

### Before / After
```java
// BEFORE — writer created before try, abort() can throw and mask original error
SSTableWriter writer = createWriter(cfs, totalSize, repairedAt);
try { ... }
catch (Throwable e) {
    writer.abort();   // if this throws, drain() never runs
    drain(dis, in.getBytesRead());
    ...
}

// AFTER — writer created inside try, abort() exception suppressed into original
SSTableWriter writer = null;
try {
    writer = createWriter(cfs, totalSize, repairedAt);
    ...
}
catch (Throwable e) {
    if (writer != null) {
        try { writer.abort(); }
        catch (Throwable e2) { e.addSuppressed(e2); }  // preserve original
    }
    drain(dis, in.getBytesRead());
    ...
}
```

---

## How to Spot
In any catch block that performs cleanup before re-throwing, every cleanup call that can itself throw must be wrapped in a nested try/catch. If the outer exception is discarded in favour of the cleanup exception, the original failure context is lost and retry logic — which may only handle specific exception types — will behave incorrectly. During review, look for naked cleanup calls (like `abort()`, `close()`, `rollback()`) directly in catch blocks without their own error handling.

---

## Test
No test added.
