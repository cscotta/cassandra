**Commit**: f9615ed275 · **Ticket**: KAFKA-17432
**Subsystem**: streams/processor/internals/DefaultStateUpdater / DefaultTaskExecutor
**Category**: Race condition / missing synchronization

**Root cause**
`DefaultStateUpdater` and `DefaultTaskExecutor` used `CountDownLatch.countDown()` to signal shutdown completion, but the latch fired before the thread's `run()` method returned. A waiting thread could observe the latch at zero, call `isRunning()` (which checked a volatile flag cleared inside `run()`), and find the thread still running. The fix replaces the latch with `Thread.join()` to wait for the OS thread to fully exit, and changes `isRunning()` to delegate to `Thread.isAlive()`.

**Before/After**
```java
// Before
shutdownLatch.countDown(); // inside run(), thread still alive
// caller: shutdownLatch.await(); then checks isRunning()

// After
thread.join(); // blocks until OS thread exits
boolean isRunning() { return thread.isAlive(); }
```

**How to spot**
A `CountDownLatch` used as a shutdown signal fires as soon as `countDown()` is called, which may be before the thread stack is fully unwound. When callers need to know the thread has fully terminated (e.g., to safely reuse resources), use `Thread.join()` instead of a latch.

**Test**
`DefaultStateUpdaterTest` and `DefaultTaskExecutorTest` updated to assert `isRunning()` returns false after shutdown completes.
