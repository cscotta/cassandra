## Commit
9a28bd23ad

## Ticket
KAFKA-19542

## Subsystem
Clients / Consumer Metrics

## Category
Resource leak

## Root cause
`FetchMetricsManager` and `RebalanceCallbackMetricsManager` were created inside `AsyncKafkaConsumer` and `ClassicKafkaConsumer` but never stored as fields, so their `close()` methods were never called. This leaked JMX/Kafka metrics sensor registrations. The fix introduces `AbstractConsumerMetricsManager` as a base that holds both managers and is closed when the consumer closes.

## Before
```java
// FetchMetricsManager created inline, no field reference, never closed
new FetchMetricsManager(metrics, ...);
```

## After
```java
// Stored as field, closed in Consumer.close()
this.fetchMetricsManager = new FetchMetricsManager(metrics, ...);
// AbstractConsumerMetricsManager base class closes both managers
```

## How to spot
Any `new XMetricsManager(...)` call whose return value is not stored is suspect. Search for `new.*MetricsManager` expressions not assigned to a field, and verify the corresponding `close()` is called.

## Test
`AsyncKafkaConsumerTest`, `ClassicKafkaConsumerTest` — verify no metrics remain registered after `consumer.close()`.
