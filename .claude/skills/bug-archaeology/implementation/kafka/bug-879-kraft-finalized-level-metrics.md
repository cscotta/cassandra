**Commit**: a64f5bf6ab · **Ticket**: KAFKA-19254

**Subsystem**: KRaft / MetadataLoader metrics

**Category**: Missing feature implementation

**Root cause**
KRaft lacked per-feature-name gauge metrics for finalized feature levels, making it impossible to monitor which feature version is currently active on a controller through standard metrics tooling. Additionally, per-node min/max supported level metrics and the ability to read the KRaft version from control records were absent. The fix adds `FinalizedLevel` gauge metrics indexed by feature name to `MetadataLoaderMetrics`, adds `NodeMetrics` for supported level ranges, and wires up control-record parsing to populate the KRaft version metric.

**Before/After**
```java
// Before: no per-feature gauge metrics
// MetadataLoaderMetrics had no FinalizedLevel gauges

// After: gauge per feature name
for (Map.Entry<String, Short> entry : finalizedFeatures.entrySet()) {
    metrics.recordFinalizedLevel(entry.getKey(), entry.getValue());
}
```

**How to spot**
After KRaft feature finalization occurs, check whether monitoring dashboards have per-feature-name gauges available. If the only metric is a single "latest features version" counter, the per-feature granularity is missing.

**Test**
`MetadataLoaderMetricsTest` and `MetadataLoaderTest` verify that `FinalizedLevel` gauges are created for each feature name after a finalized-feature control record is processed.
