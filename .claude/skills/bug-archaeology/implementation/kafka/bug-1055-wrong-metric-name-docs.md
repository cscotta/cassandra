## Commit

388db49191 — KAFKA-19287: Fix incorrect metric names and missing percentiles in ops documentation

## Ticket

KAFKA-19287

## Subsystem

Documentation / monitoring

## Category

Wrong constant / default value

## Root cause

The operations documentation in `docs/ops.html` listed the partition-count broker metric under the name `partition-count`, but the actual JMX/metrics name is `num-partitions`. Code referencing the wrong name would silently find no metric and report zero or throw a lookup failure. Additionally, the latency histogram metrics for event-queue time, processing time, purgatory-expiration time, and batch-flush time were documented without their p95 percentile quantile, while p50, p99, and p999 were present, creating an incomplete monitoring picture.

## Before / After

```html
<!-- Before — wrong metric name -->
<td>partition-count</td>

<!-- After — correct metric name -->
<td>num-partitions</td>

<!-- Before — missing p95 -->
<td>kafka.network:type=RequestMetrics,...p50,p99,p999</td>

<!-- After — p95 included -->
<td>kafka.network:type=RequestMetrics,...p50,p95,p99,p999</td>
```

## How to spot

Cross-reference every metric name in documentation against the actual `Sensor` or `MetricName` definitions in the source. A discrepancy between the documented name and the name produced by the metrics library will silently result in missing data in monitoring dashboards.

## Test

Documentation-only change; verified by manually checking metric registration in `BrokerTopicStats` and related classes.
