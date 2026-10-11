## Commit
153a458d3c

## Ticket
KAFKA-19711

## Subsystem
Kafka Streams / State Store Metrics

## Category
Wrong constant / default value

## Root cause
`MeteredKeyValueStore`, `MeteredSessionStore`, and `MeteredWindowStore` measured `commit()` using the `flushSensor`, which reports flush metrics, not commit metrics. The `commit()` operation now has its own `commitSensor` with dedicated `commit-rate`, `commit-latency-avg`, and `commit-latency-max` metrics. `flushSensor` is deprecated.

## Before
```java
// commit() path:
maybeMeasureLatency(() -> getWrappedStore().commit(...), time, flushSensor);
```

## After
```java
// commit() path:
maybeMeasureLatency(() -> getWrappedStore().commit(...), time, commitSensor);
// new metrics: commit-rate, commit-latency-avg, commit-latency-max
```

## How to spot
When a new operation is added to a class that already has sensors, check that the correct sensor is used. A common mistake is reusing the nearest existing sensor (`flushSensor`) for semantically different operations (`commit`).

## Test
`MeteredKeyValueStoreTest`, `MeteredSessionStoreTest`, `MeteredWindowStoreTest` assert that `commitSensor` is recorded on `commit()` and `flushSensor` on `flush()`.
