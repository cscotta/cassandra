## Commit

cafe83f928 — KAFKA-19275: Streams state metric returns State enum object instead of String

## Ticket

KAFKA-19275

## Subsystem

Kafka Streams / JMX metrics

## Category

Wrong serialization / deserialization

## Root cause

The `state` gauge in `KafkaStreams.ClientMetrics` was registered as `Gauge<State>`, returning the raw `State` enum object. JMX clients and JMC serialise gauge values by calling `toString()` on the returned object. Java enums override `toString()` to return the enum constant name, but JMC and some MBean tools expect a `java.lang.String` attribute type in the MBean descriptor. When the actual type is not `String`, JMC displayed "Unavailable" for the metric value. The fix changes the gauge type to `Gauge<String>` and returns `state.name()` explicitly, satisfying the JMX string-type contract.

## Before / After

```java
// Before — returns State enum, JMC shows "Unavailable"
Gauge<State> stateGauge = () -> kafkaStreams.state();
metrics.addMetric(STATE_METRIC, stateGauge);

// After — returns String, JMC displays the state name correctly
Gauge<String> stateGauge = () -> kafkaStreams.state().name();
metrics.addMetric(STATE_METRIC, stateGauge);
```

## How to spot

Search for `Gauge<` declarations in metrics registration code. Any `Gauge<EnumType>` or `Gauge<Object>` will produce an unreadable value in JMX clients that expect a primitive or `String` type. Prefer `Gauge<String>` with explicit `.name()` for enum-valued gauges.

## Test

`ClientMetricsTest` and `StreamThreadTest` were updated to assert that the `state` gauge value is a `String` equal to the expected state name.
