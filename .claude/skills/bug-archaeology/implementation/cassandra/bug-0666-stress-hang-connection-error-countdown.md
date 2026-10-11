# Bug 0666: cassandra-stress Hangs on Connection Error Due to Missed CountDown

**Commit**: 1619413e56 · **Ticket**: CASSANDRA-12938
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/StressAction.java
**Category**: Resource leak

---

## Root Cause

Each worker thread in `StressAction` calls `start.countDown()` to signal the coordinator that it is ready, and then waits on `releaseConsumers.await()` before starting actual work. The `countDown()` call was placed after the client-connection `switch` block and outside any `try/finally`. If an exception was thrown during connection setup (e.g., a refused connection or TLS failure), the thread would exit without ever calling `countDown()`. The coordinator thread was waiting for all `N` workers to count down before releasing them, so it would block indefinitely — hanging the entire stress run.

### Before / After

```java
// BEFORE — countDown skipped if switch throws
switch (clientType) {
    case JAVA_DRIVER_NATIVE: jclient = settings.getJavaDriverClient(); break;
    ...
}
// Unreachable on exception:
start.countDown();
releaseConsumers.await();
```

```java
// AFTER — countDown always called via finally
try {
    switch (clientType) {
        case JAVA_DRIVER_NATIVE: jclient = settings.getJavaDriverClient(); break;
        ...
    }
} finally {
    start.countDown();  // always released, even on error
}
releaseConsumers.await();
```

---

## How to Spot

Any `CountDownLatch.countDown()` call that is not inside a `finally` block (or equivalent) is a latent hang: any exception between entry and that line leaves the latch permanently under-counted. Review every usage of countdown/semaphore release to confirm it is protected by `finally`.

---

## Test

No test added.
