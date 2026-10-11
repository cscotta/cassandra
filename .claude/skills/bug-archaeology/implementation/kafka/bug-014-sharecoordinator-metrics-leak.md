**Commit**: b4d1ca7809 · **Ticket**: KAFKA-20347
**Subsystem**: share-coordinator — ShareCoordinatorMetrics
**Category**: Resource leak

**Root cause**
Two related bugs existed in `ShareCoordinatorMetrics`. First, when `deactivateMetricsShard(shard)` was called (e.g., on a partition leader change), the per-partition prune sensor created by `recordPrune()` was not removed from the metrics registry. With repeated leadership changes, sensors accumulated without bound. Second, the prune metric tags were constructed with a plain `Map.of(...)`, which in Java gives an unordered map; JMX metric names include tags in the iteration order of the map, making the JMX name non-deterministic across JVM runs.

**Before/After**
```java
// Before — deactivate does not clean up prune sensor
shards.remove(shard.topicPartition());
// pruneMetrics map not touched; sensor left in registry

// After — sensor removed on deactivate
ShareGroupPruneMetrics removed = pruneMetrics.remove(shard.topicPartition());
if (removed != null) {
    metrics.removeSensor(removed.pruneSensor.name());
}
// Tag construction: Map.of(...) → MetricsUtils.getTags(...) (ordered)
```

**How to spot**
Any metrics class that creates per-partition (or per-entity) sensors/metrics on a lifecycle event must remove them on the corresponding cleanup event. Search for `addReporter`/`addSensor` calls and verify a matching `removeReporter`/`removeSensor` exists in the deactivate/close path. Unordered `Map.of()` for JMX tags is a portability hazard — always use an ordered map helper.

**Test**
`ShareCoordinatorMetricsTest#testDeactivateMetricsShardCleansUpPruneMetrics` — activates a shard, records a prune event (creating the sensor), deactivates the shard, and asserts the prune metric is no longer in the registry.
