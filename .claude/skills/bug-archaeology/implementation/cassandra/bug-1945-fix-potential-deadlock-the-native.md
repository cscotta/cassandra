# Bug 1945: Potential deadlock in the native protocol request thread pool

**Commit**: 02d9238e2a · **Ticket**: CASSANDRA-5926
**Subsystem**: src/java/org/apache/cassandra/transport/RequestThreadPoolExecutor.java
**Category**: Deadlock

---

## Root Cause

`RequestThreadPoolExecutor` extended `DebuggableThreadPoolExecutor` with a bounded `LinkedBlockingQueue` of 32 tasks and a `minThreads`/`maxThreads` model. Under load, Netty's I/O threads that decode frames and submit tasks to the executor could themselves block waiting for queue space, while the executor threads were waiting for Netty to dispatch responses back through the same pipeline — a classic producer/consumer deadlock. The fix replaces the executor with Netty's `MemoryAwareThreadPoolExecutor`, which is designed to integrate safely with the Netty pipeline, uses a global in-flight limit (128 requests) instead of a strict queue bound, and converts the thread model to max-threads-only (no minimum) to remove the contention point.

### Before / After

```java
// BEFORE — extends DebuggableThreadPoolExecutor with bounded blocking queue
public class RequestThreadPoolExecutor extends DebuggableThreadPoolExecutor
{
    public RequestThreadPoolExecutor()
    {
        super(DatabaseDescriptor.getNativeTransportMinThreads(),
              DatabaseDescriptor.getNativeTransportMaxThreads(),
              CORE_THREAD_TIMEOUT_SEC, TimeUnit.SECONDS,
              new LinkedBlockingQueue<Runnable>(32),
              new NamedThreadFactory("Native-Transport-Requests"));
    }
}
```

```java
// AFTER — extends MemoryAwareThreadPoolExecutor with trivial size estimator
public class RequestThreadPoolExecutor extends MemoryAwareThreadPoolExecutor
{
    private final static int MAX_QUEUED_REQUESTS = 128;

    public RequestThreadPoolExecutor()
    {
        super(DatabaseDescriptor.getNativeTransportMaxThreads(),
              0,                  // no per-channel limit
              MAX_QUEUED_REQUESTS,
              CORE_THREAD_TIMEOUT_SEC, TimeUnit.SECONDS,
              sizeEstimator(),
              new NamedThreadFactory("Native-Transport-Requests"));
    }
}
```

---

## How to Spot

When a thread pool executor is embedded inside a Netty pipeline and uses a bounded blocking submission queue, the Netty I/O threads can deadlock waiting to enqueue work while holding resources needed to drain the queue. Any `ThreadPoolExecutor` that (a) sits inside a Netty handler chain and (b) uses a bounded blocking queue is a candidate for this pattern; prefer `MemoryAwareThreadPoolExecutor` or an unbounded queue with external back-pressure.

---

## Test

No test added.
