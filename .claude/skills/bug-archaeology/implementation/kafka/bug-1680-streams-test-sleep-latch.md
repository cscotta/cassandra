## Commit

- Hash: ebe7dd2b15
- Tickets: KAFKA-18418
- Subject: KAFKA-18418: Replace Thread.sleep with CountDownLatch in KafkaStreamsTest

## Subsystem

streams / KafkaStreamsTest

## Category

Race condition / missing synchronization

## Root cause

`KafkaStreamsTest.prepareTerminableThread()` used `Thread.sleep(2000L)` to wait for a background thread to reach a specific state before proceeding. The sleep was a timing guess — on slow CI machines the 2-second window was insufficient and the test would flake; on fast machines the sleep was wasted wall-clock time. Replacing it with a `CountDownLatch` that the thread counts down when it reaches the target state makes the synchronization deterministic.

## Before

```java
thread.start();
Thread.sleep(2000L);  // hope the thread is ready
```

## After

```java
CountDownLatch latch = new CountDownLatch(1);
KafkaStreamsWithTerminableThread streams = new KafkaStreamsWithTerminableThread(..., latch);
streams.start();
latch.await();  // guaranteed ready
```

## How to spot

`Thread.sleep()` calls in test setup or teardown that are waiting for another thread to reach a state — always replace with a `CountDownLatch`, `Semaphore`, or `CompletableFuture`.

## Test

`KafkaStreamsTest` no longer flaky in CI after the latch-based synchronization.
