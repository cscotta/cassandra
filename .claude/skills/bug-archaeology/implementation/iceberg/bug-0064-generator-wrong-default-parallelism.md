# Bug 0064: Generator operator inherits environment parallelism instead of input stream parallelism

**Commit**: 8f1f483985bfbef3c19472b9e2ec474593fee219 · **Ticket**: #15849

## Detection Rule

When an operator is chained after a `process()` call on an input `DataStream`, verify that `.setParallelism(input.getParallelism())` is explicitly set; if omitted, the operator defaults to the environment-level parallelism, which silently mismatches the upstream and causes unintended data redistribution.

## Code Smell

A `.process(...)` operator followed by `.uid(...)` and `.name(...)` with no `.setParallelism(...)` call in between, where the intent is for the operator to shadow the upstream source's parallelism rather than the job-level default.

## Tags

- scope: distributed-systems
- category: wrong-constant
- code-pattern: missing-parallelism-on-chained-operator
- severity: performance
- subsystem: flink/sink/dynamic

## Root Cause

The generator operator (the `process()` call that converts user records into `DynamicRecordInternal`) was created without an explicit parallelism setting. Flink resolves missing parallelism to the environment default, so whenever the source stream ran at a different parallelism (e.g. 8 subtasks) but the environment default was lower (e.g. 4), Flink inserted an implicit network shuffle between the source and the generator, defeating the intended 1-to-1 record routing and adding unnecessary overhead.

## Example

```java
// BEFORE
SingleOutputStreamOperator<DynamicRecordInternal> converted =
    input
        .process(new DynamicRecordProcessor<>(...))
        // missing .setParallelism(input.getParallelism())
        .uid(prefixIfNotNull(uidPrefix, "-generator"))
        .name(operatorName("generator"))
        .returns(type);
```

```java
// AFTER
SingleOutputStreamOperator<DynamicRecordInternal> converted =
    input
        .process(new DynamicRecordProcessor<>(...))
        .setParallelism(input.getParallelism())
        .uid(prefixIfNotNull(uidPrefix, "-generator"))
        .name(operatorName("generator"))
        .returns(type);
```
