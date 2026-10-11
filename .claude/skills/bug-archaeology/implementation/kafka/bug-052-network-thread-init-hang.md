**Commit**: 5cc4c87be0 · **Ticket**: KAFKA-19394
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkThread.java
**Category**: Race condition / missing synchronization

**Root cause**
`ConsumerNetworkThread.initializeResources()` ran asynchronously in a background thread without signalling the constructor whether it succeeded or failed. If initialization threw (e.g., because of an invalid JAAS config), the run-loop exited silently while the constructing thread continued, producing a consumer object whose background thread was dead. Subsequent calls like `close()` would then block forever waiting for the dead thread to respond to events.

**Before/After**
```java
// Before: run() just swallowed the error and exited
@Override
public void run() {
    try {
        initializeResources();        // failure here was only logged
        while (running) { ... }
    } catch (final Throwable e) {
        log.error("Failed to initialize resources ...", e);
    } finally { cleanup(); }
}

// After: latch + AtomicReference propagate the error to the constructor
public void start(int timeoutMs) {
    start();
    if (!initializationLatch.await(timeoutMs, MILLISECONDS))
        maybeSetInitializationError(new TimeoutException(...));
    KafkaException e = initializationError.get();
    if (e != null) throw e;
}
```

**How to spot**
Any pattern where a worker thread is launched from a constructor, performs non-trivial initialization, and provides no synchronization point back to the constructor is a hang/zombie risk. Look for `Thread.start()` calls in constructors that do not immediately `join()` or wait on a `CountDownLatch`/`CompletableFuture`.

**Test**
`testFailConstructor` uses an invalid `metric.reporter.classes` value so the constructor throws, then asserts no `NullPointerException` is logged during the resulting `close()` path.
