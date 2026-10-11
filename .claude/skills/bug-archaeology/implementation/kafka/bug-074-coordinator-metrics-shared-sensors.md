**Commit**: 08eda2ebed · **Ticket**: KAFKA-19445
**Subsystem**: coordinator-common/src/main/java/org/apache/kafka/coordinator/common/runtime/CoordinatorRuntimeMetricsImpl.java
**Category**: State not cleaned up

**Root cause**
`CoordinatorRuntimeMetricsImpl` registered sensors with generic names like `"GroupPartitionLoadTime"`, `"ThreadIdleRatio"`, `"EventQueueTime"`, etc. In Kafka's `Metrics` framework, sensors with the same name are shared across all instances. When two coordinator runtimes (e.g., the group coordinator and the share coordinator) instantiated `CoordinatorRuntimeMetricsImpl` with different `metricsGroup` values, they shared the same underlying sensors. Any data recorded by one runtime was added to the metrics of both, making it impossible to distinguish per-coordinator values.

**Before/After**
```java
// Before: generic sensor names shared across all metric groups
this.partitionLoadSensor = metrics.sensor("GroupPartitionLoadTime");
this.threadIdleSensor    = metrics.sensor("ThreadIdleRatio");
this.eventQueueTimeSensor = metrics.sensor("EventQueueTime");

// After: sensor names prefixed with the metrics group to isolate them
this.partitionLoadSensor  = metrics.sensor(this.metricsGroup + "-PartitionLoadTime");
this.threadIdleSensor     = metrics.sensor(this.metricsGroup + "-ThreadIdleRatio");
this.eventQueueTimeSensor = metrics.sensor(this.metricsGroup + "-EventQueueTime");
```

**How to spot**
When a metrics helper class is instantiated multiple times (once per subsystem/group), sensor names must include a discriminating prefix. Grep for `metrics.sensor("` with literal string arguments in classes that can be instantiated with different group names, and verify the name includes the group.

**Test**
`testPartitionLoadSensorMetricsGroupIsolation` (and similar isolation tests for each sensor type) creates two `CoordinatorRuntimeMetricsImpl` instances with different group names, records data on only one, and asserts the other group's metrics remain at `Double.NaN` / 0.
