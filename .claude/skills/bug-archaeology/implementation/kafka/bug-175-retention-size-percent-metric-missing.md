**Commit**: 0a9d2dccbd · **Ticket**: KAFKA-20157
**Subsystem**: storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java
**Category**: Missing null / bounds check

**Root cause**
KIP-1257 introduced a `RetentionSizeInPercent` JMX gauge for tiered-storage topics via `RemoteLogManager`, but no equivalent metric existed for regular (non-tiered) topics with size-based retention. Operators monitoring disk usage had no standardised percentage metric for local-only topics. The fix adds an `AtomicInteger retentionSizeInPercentValue` field to `UnifiedLog`, exposes it as a Yammer gauge, and updates it in the `finally` block of `deleteOldSegments()` so the metric always reflects the current partition size as a percentage of `retention.bytes`, even when segment deletion fails.

**Before/After**
```java
// Before: no RetentionSizeInPercent gauge for non-tiered topics
metricsGroup.newGauge(LogMetricNames.SIZE, this::size, tags);
// (no percentage metric)

// After: new gauge registered and updated during log cleanup
metricsGroup.newGauge(LogMetricNames.RETENTION_SIZE_IN_PERCENT,
    retentionSizeInPercentValue::get, tags);
// Updated in finally block of deleteOldSegments():
retentionSizeInPercentValue.set(calculateRetentionSizeInPercent());
```

**How to spot**
When a new monitoring metric is added for one storage mode (tiered), check whether an equivalent metric is needed for the non-tiered mode. Missing metric parity creates gaps in observability dashboards and forces operators to use different monitoring strategies depending on configuration.

**Test**
`UnifiedLogTest` parameterised with `@CsvSource` covering: remote storage enabled with copy enabled (returns 0), remote storage enabled but copy disabled (metric calculated), and no remote storage (metric calculated). Tests verify gauge values via Yammer metrics inspection and assert correct percentage calculations for unlimited retention configuration.
