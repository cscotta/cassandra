## Commit

0076b65f99 — KAFKA-19182: Move SchedulerTest from Scala core to Java server module

## Ticket

KAFKA-19182

## Subsystem

Server utilities / scheduler

## Category

Missing null / bounds check

## Root cause

`SchedulerTest` was in the `core` module written in Scala. Several test cases used `Thread.sleep` to wait for scheduled tasks, making them inherently racy: a slow CI machine could wake up before the scheduled task executed, causing intermittent failures. Rewriting the tests in Java using `TestUtils.waitForCondition` and moving them to the `server` module (which owns `KafkaScheduler`) makes the test boundaries explicit and eliminates the sleep-based timing assumptions. The move also aligns the test with the module that owns the code under test.

## Before / After

```scala
// Before — sleep-based wait, racey on slow machines
scheduler.schedule("test", () => counter.incrementAndGet(), 100, -1)
Thread.sleep(200)
assertEquals(1, counter.get())
```

```java
// After — condition-based wait, deterministic
scheduler.schedule("test", () -> counter.incrementAndGet(), 100, -1, TimeUnit.MILLISECONDS);
TestUtils.waitForCondition(() -> counter.get() >= 1, "Task not executed");
assertEquals(1, counter.get());
```

## How to spot

Search for `Thread.sleep` calls in scheduler or timer tests. Any test that sleeps a fixed duration to wait for a scheduled callback is inherently flaky; replace with `waitForCondition` to make the test pass reliably under variable load.

## Test

`SchedulerTest.java` in the `server` module replaces all sleep-based assertions with condition-based waits.
