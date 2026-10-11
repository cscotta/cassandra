# Bug 2031: Compaction Throttle Applied in Bursty 1000-Row Batches Instead of Per-Buffer-Read

**Commit**: 2b0797b24e · **Ticket**: CASSANDRA-4316
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java, src/java/org/apache/cassandra/db/compaction/CompactionIterable.java, src/java/org/apache/cassandra/db/compaction/ParallelCompactionIterable.java, src/java/org/apache/cassandra/db/compaction/Scrubber.java, src/java/org/apache/cassandra/io/sstable/SSTableReader.java, src/java/org/apache/cassandra/io/sstable/SSTableScanner.java, src/java/org/apache/cassandra/io/util/ThrottledReader.java, src/java/org/apache/cassandra/io/compress/CompressedThrottledReader.java
**Category**: Wrong constant / default value

---

## Root Cause

Compaction throughput throttling used a custom `Throttle` class that was called only every 1,000 rows processed. Between throttle points the compaction could read at full disk speed, creating large I/O bursts. Additionally, the per-stream throttle target was recomputed by dividing the configured MB/s limit by the number of active compactions, which meant the limit was not enforced globally and could drift as compactions started or finished.

The fix replaces the row-counted `Throttle` with a Guava `RateLimiter` held on `CompactionManager` and shared across all scanners. `ThrottledReader` and `CompressedThrottledReader` call `limiter.acquire(buffer.length)` inside `reBuffer()` — throttling at each actual disk read rather than every 1,000 rows — giving smooth, globally-correct throughput limiting.

### Before / After

```java
// BEFORE — throttle called every 1000 rows in CompactionIterable
if ((row++ % 1000) == 0)
{
    long n = 0;
    for (ICompactionScanner scanner : scanners)
        n += scanner.getCurrentPosition();
    bytesRead = n;
    controller.mayThrottle(bytesRead);
}
```

```java
// AFTER — bytesRead updated every row; throttle enforced per buffer read inside ThrottledReader
long n = 0;
for (ICompactionScanner scanner : scanners)
    n += scanner.getCurrentPosition();
bytesRead = n;
```

```java
// ThrottledReader.reBuffer (new)
protected void reBuffer()
{
    limiter.acquire(buffer.length);
    super.reBuffer();
}
```

---

## How to Spot

Throttle logic that fires on row or record counts rather than on bytes actually transferred will always be bursty because row sizes vary widely. I/O throttling should be applied at the byte-transfer layer (the buffer read/write call), not at a higher-level record-counting loop. Review any `if ((counter++ % N) == 0) throttle(...)` pattern in I/O paths.

---

## Test

No test added.
