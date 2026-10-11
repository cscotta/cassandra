## Commit
ba96e092fe

## Ticket
KAFKA-20211 / KAFKA-20345

## Subsystem
Coordinator / Metrics

## Category
Race condition / missing synchronization

## Root cause
`CoordinatorBackgroundThreadPoolExecutorTest` called `verify(metrics, times(N))` before all submitted tasks had completed, causing sporadic assertion failures. The task counter was asserted right after submission but the executor's threads ran asynchronously.

## Before
```java
// submit tasks, then immediately:
verify(metrics, times(expectedCount)).record(...);
```

## After
```java
// wait for metric recording with a timeout:
verify(metrics, timeout(5000).times(1)).record(...);
// final assertions after awaitTermination
executor.shutdown();
executor.awaitTermination(5, SECONDS);
assertEquals(...);
```

## How to spot
Any `verify(mock, times(N))` immediately following asynchronous task submission without an explicit synchronization barrier. Replace with `timeout()` or `CountDownLatch`.

## Test
`CoordinatorBackgroundThreadPoolExecutorTest` — the test itself was the fix target; it now passes reliably.
