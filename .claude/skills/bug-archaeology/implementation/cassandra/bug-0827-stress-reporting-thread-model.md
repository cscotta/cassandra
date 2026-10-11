# Bug 0827: cassandra-stress reporting runs on worker thread, losing precision

**Commit**: e73633cd81 · **Ticket**: CASSANDRA-12585
**Subsystem**: cassandra-stress / metrics reporting
**Category**: Race condition / missing synchronization

---

## Root Cause
The old `StressMetrics` lived in the same package as the stress workers and accumulated timing data via direct, shared `Timing` objects that every worker thread wrote to concurrently without any lock-free discipline. This meant that latency samples taken inside each worker could be lost or double-counted because the reporting thread drained the same mutable state at an arbitrary moment. The fix introduces a single-producer/single-consumer (`SpscArrayQueue` / `SpscUnboundedArrayQueue`) per-consumer pipeline: each worker thread records completed operations into its own lock-free queue using object recycling, and a dedicated `StressMetrics` reporting thread drains those queues at the logging interval. This eliminates shared mutable state between worker and reporter, ensuring every operation is counted exactly once at full nanosecond precision.

### Before / After
```java
// BEFORE — consumer receives a shared OpDistribution linked to shared Timing
consumers[i] = new Consumer(
    operations.get(metrics.getTiming(), isWarmup), done, ...);

// AFTER — consumer owns lock-free queues; metrics subscribes to consumer
consumers[i] = new Consumer(operations, isWarmup, done, ...);
// inside Consumer constructor:
this.measurementsRecycling = new SpscArrayQueue<>(8 * 1024);
this.measurementsReporting = new SpscUnboundedArrayQueue<>(2048);
metrics.add(this);
```

---

## How to Spot
Any benchmarking or metrics tool where worker threads write directly into a shared histogram or counter without a lock is vulnerable to lost updates and skewed latency percentiles. Look for patterns where `getTiming()` or `getHistogram()` returns the same object instance to multiple threads — even if the object is thread-safe at the field level, the aggregated view may be incoherent across threads if the reporter resets state mid-flight.

---

## Test
No new test added for the thread-model change itself; the existing stress test suite exercises the execution path.
