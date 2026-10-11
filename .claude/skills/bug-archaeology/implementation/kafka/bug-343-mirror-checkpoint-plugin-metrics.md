## Commit
b2048a5908

## Ticket
KAFKA-19149

## Subsystem
MirrorMaker / Metrics

## Category
Wrong constant / default value

## Root cause
`MirrorCheckpointTask` used a single internal `Metrics` instance for legacy JMX metrics. KIP-877 introduces `PluginMetrics`, the standard Connect metrics API. Before this change, `MirrorCheckpointMetrics` was not wired to `PluginMetrics`, so checkpoint-latency metrics were invisible to Connect's metrics reporters. The fix introduces dual-format support: `MirrorCheckpointLegacyMetrics` (old JMX format) and a new `MirrorCheckpointMetrics` backed by `PluginMetrics`. A `metric.names.format` config controls which formats are emitted.

## Before
```java
// MirrorCheckpointTask
metrics = config.metrics();  // always legacy JMX Metrics instance
```

## After
```java
List<String> formats = config.metricNamesFormats();
legacyMetrics = formats.contains(METRIC_NAMES_LEGACY) ? config.legacyMetrics() : null;
metrics = formats.contains(METRIC_NAMES_NEW) ? config.metrics(context.pluginMetrics()) : null;
```

## How to spot
Any MirrorMaker task that creates a raw `new Metrics()` rather than using `context.pluginMetrics()` is missing the standard Connect metrics plumbing. Check `commitRecord()` implementations for correct metric fan-out.

## Test
`MirrorCheckpointTaskTest` verifies both legacy and new metric formats are recorded when both are enabled.
