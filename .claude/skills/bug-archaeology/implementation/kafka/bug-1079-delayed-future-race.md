## Commit

053b9e423c — KAFKA-19091: DelayedFutureTest has race condition checking expiry result

## Ticket

KAFKA-19091

## Subsystem

Server / purgatory DelayedFuture

## Category

Race condition / missing synchronization

## Root cause

`DelayedFutureTest` scheduled a `DelayedFuture` that incremented a counter on expiry, then immediately called `result.get()` on the next line without waiting for the expiry callback thread to run. Because the callback executes on a timer thread, the assertion could fire before the counter was incremented, causing intermittent `AssertionError: expected 40 but was 0`. Additionally, the callback was typed as `Function<List, Void>` which required the lambda to explicitly return `null`, making the null return a potential source of confusion and static-analysis warnings.

## Before / After

```java
// Before — race: checks result immediately without waiting
delayedFuture.expire();
assertEquals(40, result.get());  // callback may not have run yet

// After — waits for condition on callback thread
delayedFuture.expire();
TestUtils.waitForCondition(() -> result.get() == 40, "Expiry callback not executed");

// Before — Function<List, Void> requires explicit null return
Function<List<Integer>, Void> onExpire = list -> { sum.set(...); return null; };

// After — Consumer<List> avoids null return
Consumer<List<Integer>> onExpire = list -> sum.set(...);
```

## How to spot

In purgatory or timer-based tests, any assertion that follows `expire()` or `tryComplete()` without a `waitForCondition` is a race. The callback executes on a separate thread and may not have completed by the time the assertion runs.

## Test

The fixed `DelayedFutureTest` uses `TestUtils.waitForCondition` for all post-expiry assertions, eliminating the flakiness.
