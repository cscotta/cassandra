# Bug 0751: Speculative retry threshold uses wrong units

**Commit**: 25938090a6 · **Ticket**: CASSANDRA-13009
**Subsystem**: Read path / Speculative execution
**Category**: Wrong constant / default value

---

## Root Cause
Two unit-mismatch bugs in `ColumnFamilyStore` caused speculative retry to fire at wildly incorrect thresholds. First, the initial value of `sampleLatencyNanos` was assigned directly from `DatabaseDescriptor.getReadRpcTimeout()`, which returns milliseconds, without converting to nanoseconds — making the initial threshold 1,000,000x too small. Second, when the threshold was recomputed from a latency percentile, the snapshot value (already in nanoseconds from the metrics histogram) was multiplied by `1000d` as if it were in microseconds, inflating the effective threshold by 1,000x. Both errors caused speculative retry to either never fire (threshold too high) or fire on every read (threshold too low) depending on actual latencies.

### Before / After
```java
// BEFORE — milliseconds assigned to a nanos field
sampleLatencyNanos = DatabaseDescriptor.getReadRpcTimeout() / 2;

// AFTER — explicit milliseconds-to-nanoseconds conversion
sampleLatencyNanos = TimeUnit.MILLISECONDS.toNanos(DatabaseDescriptor.getReadRpcTimeout() / 2);

// BEFORE — percentile value (nanoseconds) multiplied as if microseconds
sampleLatencyNanos = (long) (metric.coordinatorReadLatency.getSnapshot().getValue(retryPolicy.value) * 1000d);

// AFTER — percentile value used directly
sampleLatencyNanos = (long) (metric.coordinatorReadLatency.getSnapshot().getValue(retryPolicy.value));
```

---

## How to Spot
Fields named `*Nanos` should only ever be assigned values that are already in nanoseconds. In particular, `DatabaseDescriptor` timeout getters return milliseconds; any assignment to a nanosecond field must include an explicit `TimeUnit.MILLISECONDS.toNanos()` call. Metrics snapshot `getValue()` for latency histograms returns nanoseconds — multiplying by 1000 is a unit error that will cause the speculative retry percentile threshold to be 1000x larger than intended.

---

## Test
No test added.
