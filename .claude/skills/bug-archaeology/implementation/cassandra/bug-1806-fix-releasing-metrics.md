# Bug 1806: CF Metrics Release Misses AllMemtablesDataSize and Uses Wrong SpeculativeRetries Name

**Commit**: 5c86f36216 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/metrics/ColumnFamilyMetrics.java
**Category**: Resource leak

---

## Root Cause

`ColumnFamilyMetrics.release()` was responsible for unregistering all metrics from the Codahale registry when a column family was dropped or removed. Two errors existed: `AllMemtablesDataSize` was registered on construction but never removed in `release()`, leaving a leaked metric in the registry; and the metric registered as `SpeculativeRetries` was being looked up for removal under the name `SpeculativeRetry` (missing the trailing "s"), so the unregister call silently did nothing and the metric remained in the registry indefinitely.

### Before / After

```java
// BEFORE – AllMemtablesDataSize never removed; wrong name for SpeculativeRetries
Metrics.defaultRegistry().removeMetric(factory.createMetricName("SpeculativeRetry"));
```

```java
// AFTER – AllMemtablesDataSize removed; correct name used
Metrics.defaultRegistry().removeMetric(factory.createMetricName("AllMemtablesDataSize"));
...
Metrics.defaultRegistry().removeMetric(factory.createMetricName("SpeculativeRetries"));
```

---

## How to Spot

Every metric registered in a constructor or `init` method must have a matching `removeMetric` call in the corresponding `release`/`close` method. A straightforward audit is to diff the set of `createMetricName` strings at registration time against those at release time—any asymmetry is a leak. Additionally, metric names must be copied exactly; a typo causes the removal to silently no-op.

---

## Test

No test added.
