**Commit**: bb95e3ab19 · **Ticket**: KAFKA-19882

**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/metrics/StreamsMetricsImpl.java

**Category**: Wrong constant/default value

**Root cause**
During KIP-1091 development, `process-id` and `application-id` tags were accidentally applied to every client-level metric through `StreamsMetricsImpl` constructor parameters rather than being added only to the specific metrics that KIP-1091 mandated (`client-state` and `recording-level`). This caused all Streams client-level metrics to carry these extra tags, polluting time-series data and breaking dashboards that grouped metrics by tag cardinality. The fix removes `processId` and `applicationId` from the `StreamsMetricsImpl` constructor and instead passes them as explicit parameters only to `addClientStateTelemetryMetric` and `addClientRecordingLevelMetric`.

**Before**
```java
// Before: all metrics received process-id and application-id
new StreamsMetricsImpl(metrics, clientId,
    processId.toString(), applicationId, time);
```

**After**
```java
// After: tags scoped to specific metrics only
new StreamsMetricsImpl(metrics, clientId, time);
ClientMetrics.addClientStateTelemetryMetric(
    processId.toString(), applicationId, streamsMetrics, ...);
```

**How to spot**
In `StreamsMetricsImpl`, look for constructor parameters that are injected into the tag map for every metric registration; if those parameters belong only to a small subset of metrics (as specified by a KIP), they should be scoped to those metrics' registration calls instead.

**Test**
Create a `StreamsMetricsImpl` instance and list all registered metric names; assert that only `client-state` carries `application-id` and `process-id` tags, and that no other client-level metric does.
