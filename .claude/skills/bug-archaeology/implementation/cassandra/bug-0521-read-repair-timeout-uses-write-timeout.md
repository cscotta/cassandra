# Bug 0521: Read Repair Mutations Use Write Timeout Instead of Read Timeout

**Commit**: 9abe2127dd · **Ticket**: CASSANDRA-15442
**Subsystem**: src/java/org/apache/cassandra/service/reads/repair/BlockingPartitionRepair.java
**Category**: Wrong constant / default value

---

## Root Cause

`BlockingReadRepair` waited for blocking read-repair acknowledgements using `DatabaseDescriptor.getWriteRpcTimeout()` instead of the read timeout, and computed the remaining wait time relative to when mutations were sent (`mutationsSentTime`) rather than relative to when the original client read request started (`queryStartNanoTime`). This meant that read repair could extend the overall read operation by up to an additional full write timeout beyond the read deadline, causing clients to observe latencies far exceeding `read_request_timeout_in_ms`.

The fix renames `awaitRepairs(long timeout, TimeUnit)` to `awaitRepairsUntil(long timeoutAt, TimeUnit)`, changing the semantics from a duration to an absolute deadline, and passes `queryStartNanoTime + readRpcTimeout` as that deadline so the repair respects the same budget as the overall read.

### Before / After

```java
// BEFORE — awaits for a full write-timeout duration starting from when mutations were sent
public boolean awaitRepairs(long timeout, TimeUnit timeoutUnit)
{
    long elapsed = System.nanoTime() - mutationsSentTime;
    long remaining = timeoutUnit.toNanos(timeout) - elapsed;
    return latch.await(remaining, TimeUnit.NANOSECONDS);
}
// caller:
repair.awaitRepairs(DatabaseDescriptor.getWriteRpcTimeout(NANOSECONDS), NANOSECONDS)
```

```java
// AFTER — awaits until an absolute future time
public boolean awaitRepairsUntil(long timeoutAt, TimeUnit timeUnit)
{
    long remaining = timeUnit.toNanos(timeoutAt) - System.nanoTime();
    return latch.await(remaining, TimeUnit.NANOSECONDS);
}
// caller:
repair.awaitRepairsUntil(DatabaseDescriptor.getReadRpcTimeout(NANOSECONDS) + queryStartNanoTime, NANOSECONDS)
```

---

## How to Spot

Whenever repair or background work that is logically part of a read is bounded by a write timeout, the actual client-visible latency can be `read_timeout + write_timeout`. Review any latch/barrier `await` calls inside read path code and verify that (a) the timeout value comes from the read configuration, not the write configuration, and (b) the deadline is anchored to the original request start time rather than to when the secondary operation was dispatched.

---

## Test

`readRepairTimeoutTest` in `DistributedReadWritePathTest` drops READ_REPAIR_RSP messages to node 1, issues a CL=ALL SELECT, and asserts that a `ReadTimeoutException` is thrown within a narrow window around `read_request_timeout_in_ms`.
