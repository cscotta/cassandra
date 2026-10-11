# Bug 2354: REPLICATE_ON_WRITE stage uses unbounded queue, allowing tasks to pile up indefinitely

**Commit**: 2ca2fb3fdc · **Ticket**: CASSANDRA-2889

## Detection Rule

When a `ThreadPoolExecutor` stage is created with a `LinkedBlockingQueue` that has no capacity bound, check whether the workload feeding that stage can produce tasks faster than they are consumed — an unbounded queue hides back-pressure, exhausts heap, and causes GC storms.

## Code Smell

A stage is initialised as `multiThreadedConfigurableStage(Stage.REPLICATE_ON_WRITE, getConcurrentReplicators())` where the overload used defaults to an unbounded `LinkedBlockingQueue`, while other high-traffic stages (MUTATION, READ) are also unbounded — one of them being `REPLICATE_ON_WRITE` which fires once per CL.ONE write.

## Tags

- scope: cassandra
- category: resource-leak
- code-pattern: unbounded-executor-queue
- severity: performance
- subsystem: network-messaging

## Root Cause

At `CL.ONE`, every write triggers a replicate-on-write task (for counter tables or read repair) submitted to the `REPLICATE_ON_WRITE` stage. Because that stage used an unbounded `LinkedBlockingQueue`, a burst of writes could enqueue millions of tasks, exhausting heap. The fix creates the stage with a bounded queue of `1024 * availableProcessors` tasks and introduces a new `multiThreadedConfigurableStage` overload that accepts the `maxTasksBeforeBlock` argument.

## Example

```java
// BEFORE
stages.put(Stage.REPLICATE_ON_WRITE,
        multiThreadedConfigurableStage(Stage.REPLICATE_ON_WRITE, getConcurrentReplicators()));
// uses unbounded LinkedBlockingQueue internally
```

```java
// AFTER
public static final int MAX_REPLICATE_ON_WRITE_TASKS = 1024 * Runtime.getRuntime().availableProcessors();
stages.put(Stage.REPLICATE_ON_WRITE,
        multiThreadedConfigurableStage(Stage.REPLICATE_ON_WRITE,
                getConcurrentReplicators(), MAX_REPLICATE_ON_WRITE_TASKS));
// uses new LinkedBlockingQueue<>(maxTasksBeforeBlock)
```
