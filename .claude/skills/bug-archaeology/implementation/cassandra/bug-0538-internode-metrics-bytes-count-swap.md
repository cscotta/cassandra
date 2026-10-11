# Bug 0538: InternodeOutboundMetrics Bytes/Count Metric Reference Swapped

**Commit**: 0622288c9b · **Ticket**: CASSANDRA-15186
**Subsystem**: `src/java/org/apache/cassandra/metrics/InternodeOutboundMetrics.java`
**Category**: Wrong constant / default value

---

## Root Cause

When registering the "DroppedDueToOverload" metrics for both the large-message and urgent-message pools, the method references for `overloadedBytes` and `overloadedCount` were assigned to the opposite metric names: the metric named `…DroppedTasksDueToOverload` (a count) was wired to `::overloadedBytes`, and the metric named `…DroppedBytesDueToOverload` (bytes) was wired to `::overloadedCount`. This copy-paste inversion meant that any monitoring dashboard or alert watching these metrics would see the wrong values—byte totals reported as task counts and vice versa.

### Before / After

```java
// BEFORE
largeMessageDroppedTasksDueToOverload = Metrics.register(
    factory.createMetricName("LargeMessageDroppedTasksDueToOverload"),
    messagingPool.large::overloadedBytes);   // WRONG
largeMessageDroppedBytesDueToOverload = Metrics.register(
    factory.createMetricName("LargeMessageDroppedBytesDueToOverload"),
    messagingPool.large::overloadedCount);   // WRONG
```

```java
// AFTER
largeMessageDroppedTasksDueToOverload = Metrics.register(
    factory.createMetricName("LargeMessageDroppedTasksDueToOverload"),
    messagingPool.large::overloadedCount);   // correct
largeMessageDroppedBytesDueToOverload = Metrics.register(
    factory.createMetricName("LargeMessageDroppedBytesDueToOverload"),
    messagingPool.large::overloadedBytes);   // correct
```

---

## How to Spot

When reviewing metric registration blocks where pairs of related metrics are registered in adjacent lines (e.g., `…Tasks` / `…Bytes`), verify that the metric name suffix matches the supplier method suffix on the same line. A naming mismatch between the string key and the method reference is invisible at runtime—the metric just silently reports the wrong thing—so code review or a naming-convention lint rule is the only reliable check.

---

## Test

No test added.
