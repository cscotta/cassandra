# Bug 2019: Integer Overflow in Batch Commit Log Sync Window Comparison

**Commit**: dac6992661 · **Ticket**: CASSANDRA-5584
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/BatchCommitLogExecutorService.java
**Category**: Off-by-one

---

## Root Cause

The batch commit log executor computed the end-of-window deadline as `System.nanoTime() + window_nanos` and then compared `System.nanoTime() < end`. Because `System.nanoTime()` can wrap around (it is a relative counter and may be negative or near `Long.MAX_VALUE`), the addition `nanoTime + window` could overflow to a negative value, causing the loop to exit immediately or run far longer than intended. The fix avoids the overflow by comparing elapsed time (`nanoTime - start < window`) instead of an absolute deadline.

### Before / After

```java
// BEFORE — overflow risk when nanoTime is near Long.MAX_VALUE
long end = System.nanoTime() + (long)(1000000 * DatabaseDescriptor.getCommitLogSyncBatchWindow());
...
while (...&& System.nanoTime() < end)
```

```java
// AFTER — elapsed comparison is overflow-safe
long start = System.nanoTime();
long window = (long)(1000000 * DatabaseDescriptor.getCommitLogSyncBatchWindow());
...
while (...&& System.nanoTime() - start < window)
```

---

## How to Spot

Whenever a deadline is computed by adding a duration to `System.nanoTime()`, check for potential overflow. The idiomatic safe pattern is to capture `start` and compare `System.nanoTime() - start < duration`, which works correctly even when the nanosecond counter wraps around.

---

## Test

No test added.
