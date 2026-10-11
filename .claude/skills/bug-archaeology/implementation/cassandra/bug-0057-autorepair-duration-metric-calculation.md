# Bug 0057: AutoRepair Duration Metric Reports Zero When Schedule Finishes Quickly

**Commit**: f6eb4a6b31 · **Ticket**: CASSANDRA-20622
**Subsystem**: repair/autorepair
**Category**: Logic error in condition

---

## Root Cause

`AutoRepair` tracks elapsed repair time by storing `repairScheduleElapsedInMillis` earlier in the schedule loop, but that variable was computed as the difference before the code entered the "sleep if finished quickly" branch. When a repair schedule completes very quickly the code then sleeps up to `SLEEP_IF_REPAIR_FINISHES_QUICKLY` to pad the cycle, but `repairScheduleElapsedInMillis` was never updated to include that sleep time. The metric was therefore set from a stale snapshot rather than the true wall-clock duration of the entire schedule cycle.

### Before / After

```java
// BEFORE
repairState.setNodeRepairTimeInSec((int) TimeUnit.MILLISECONDS.toSeconds(repairScheduleElapsedInMillis));
```

```java
// AFTER
repairState.setNodeRepairTimeInSec((int) TimeUnit.MILLISECONDS.toSeconds(timeFunc.get() - startTimeInMillis));
```

---

## How to Spot

Whenever a metric is computed from an intermediate captured value instead of re-reading a clock at the point the measurement is taken, look for paths that add latency (sleeps, retries, waits) between the capture and the metric assignment. If any such path exists but the metric still uses the early capture, the reported duration will be systematically under-counted on those paths.

---

## Test

`AutoRepairSchedulerTest` asserts that `nodeRepairTimeInSec` is `greaterThanOrEqualTo(2L)` (matching the configured `SLEEP_IF_REPAIR_FINISHES_QUICKLY`) rather than merely `greaterThan(0L)`.
