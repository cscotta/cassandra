# Bug 0061: Forward-writer operator uses wrong parallelism, breaking operator chaining

**Commit**: f66305aecb789e7583f26ccbdb922ba1c8175f1c · **Ticket**: #16026

## Detection Rule

When a new operator is appended via `transform()` or a `SinkWriterOperatorFactory` to a side-output stream, verify that `.setParallelism()` is explicitly called to match the upstream stream's parallelism; omitting it lets Flink inherit the environment default, which prevents operator chaining and changes data routing.

## Code Smell

A `.transform(...)` or `.sinkTo(...)` call on a `getSideOutput(...)` stream that is followed immediately by `.uid(...)` with no intervening `.setParallelism(...)`, while the design intent is that the operator must be chained (same parallelism) with its upstream.

## Tags

- scope: distributed-systems
- category: wrong-constant
- code-pattern: missing-parallelism-on-chained-operator
- severity: performance
- subsystem: flink/sink/dynamic

## Root Cause

The `Forward-Writer` operator was appended to the forward side-output stream without inheriting `converted.getParallelism()`. Flink then assigned the environment-level default parallelism to the operator, which differs from the upstream operator's parallelism and therefore prevents the two operators from being chained. The forward-write path was deliberately designed to avoid data shuffle via Flink operator chaining, so any parallelism mismatch completely defeats its purpose.

## Example

```java
// BEFORE
converted
    .getSideOutput(new OutputTag<>(DYNAMIC_FORWARD_STREAM, sideOutputType))
    .transform(
        operatorName("Forward-Writer"),
        writeResultTypeInfo,
        new SinkWriterOperatorFactory<>(forwardWriterSink))
    // missing .setParallelism(converted.getParallelism())
    .uid(prefixIfNotNull(uidPrefix, "-forward-writer"));
```

```java
// AFTER
converted
    .getSideOutput(new OutputTag<>(DYNAMIC_FORWARD_STREAM, sideOutputType))
    .transform(
        operatorName("Forward-Writer"),
        writeResultTypeInfo,
        new SinkWriterOperatorFactory<>(forwardWriterSink))
    .setParallelism(converted.getParallelism())
    .uid(prefixIfNotNull(uidPrefix, "-forward-writer"));
```
