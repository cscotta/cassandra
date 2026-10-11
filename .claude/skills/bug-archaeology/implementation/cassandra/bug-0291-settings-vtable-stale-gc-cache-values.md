# Bug 0291: Settings Virtual Table Shows Stale Values for GC Thresholds and Cache Sizes

**Commit**: 04901913d6 · **Ticket**: CASSANDRA-17737
**Subsystem**: config / virtual tables / gc-inspector
**Category**: State not cleaned up

---

## Root Cause

Several configuration properties computed at startup—`prepared_statements_cache_size_mb`, `key_cache_size_in_mb`, `counter_cache_size_in_mb`, and `index_summary_capacity_in_mb`—were derived from heuristics (e.g., 5% of heap) and stored in local variables without writing the computed value back to `Config`. The Settings virtual table reads `Config` fields directly, so it always reflected the YAML-configured value (often `null` or a default) rather than the effective value actually in use. Similarly, `GCInspector` held its own private `gcLogThreshholdInMs` / `gcWarnThreasholdInMs` fields instead of delegating to `DatabaseDescriptor`, meaning JMX updates through `GCInspector` were invisible to the Settings virtual table.

### Before / After

```java
// BEFORE - GCInspector owned private copies of thresholds
private volatile long gcLogThreshholdInMs = DatabaseDescriptor.getGCLogThreshold();
private volatile long gcWarnThreasholdInMs = DatabaseDescriptor.getGCWarnThreshold();
// ...
gcWarnThreasholdInMs = threshold; // only updates local field
```

```java
// AFTER - GCInspector delegates to DatabaseDescriptor
public void setGcWarnThresholdInMs(long threshold)
{
    // ... validation ...
    DatabaseDescriptor.setGCWarnThreshold((int) threshold);
}
public long getGcWarnThresholdInMs()
{
    return DatabaseDescriptor.getGCWarnThreshold();
}
```

---

## How to Spot

Any JMX-settable property that stores its authoritative value in an instance field rather than in `DatabaseDescriptor` / `Config` creates a shadow copy that diverges from what the Settings virtual table reads. During review, verify that each `setXxx` method in a service class either mutates `conf.*` directly or calls a `DatabaseDescriptor.setXxx()` delegate. Also confirm that startup initialization code writes computed defaults back to `conf` so that the virtual table sees the effective value, not the raw YAML value.

---

## Test

`GCInspectorTest.setThresholds()` is extended to assert that `DatabaseDescriptor.getGCLogThreshold()` and `DatabaseDescriptor.getGCWarnThreshold()` reflect the values set through `GCInspector`'s JMX interface.
