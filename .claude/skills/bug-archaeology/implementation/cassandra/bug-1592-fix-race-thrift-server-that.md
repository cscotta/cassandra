# Bug 1592: Fix Race in Thrift Server That Can Exhaust the Pool of Worker Threads

**Commit**: f727381253 · **Ticket**: CASSANDRA-6788
**Subsystem**: src/java/org/apache/cassandra/thrift/CustomTThreadPoolServer.java
**Category**: Race condition / missing synchronization

---

## Root Cause

In `CustomTThreadPoolServer`, the `activeClients` counter was decremented before the transport streams were closed in the worker thread's `finally` block. This created a window where the acceptor loop could see the decremented count and accept a new connection, then attempt to submit it to the thread pool—while the exiting worker thread was still in the process of closing. If the thread pool was at capacity, this resulted in a `RejectedExecutionException` being thrown and uncaught, leaking the accepted connection and causing the pool to appear exhausted. Additionally, the retry sleep was 100ms, making the problem worse under load.

### Before / After

```java
// BEFORE: counter decremented before streams closed; no RejectedExecutionException handling
finally
{
    activeClients.decrementAndGet();
    if (socket != null)
        ThriftSessionManager.instance.connectionComplete(socket);
}
if (inputTransport != null)
    inputTransport.close();
if (outputTransport != null)
    outputTransport.close();
```

```java
// AFTER: streams closed before decrement; RejectedExecutionException caught and handled
finally
{
    if (socket != null)
        ThriftSessionManager.instance.connectionComplete(socket);
    if (inputTransport != null)
        inputTransport.close();
    if (outputTransport != null)
        outputTransport.close();
    activeClients.decrementAndGet();  // last step
}
// In acceptor loop:
catch (RejectedExecutionException e)
{
    logger.debug("Dropping client connection because our limit of {} has been reached", args.maxWorkerThreads);
    continue;
}
```

---

## How to Spot

Whenever a counter is decremented (or a resource "released") before associated cleanup completes, there is a window where another thread can observe the freed slot and attempt to reuse it prematurely. In thread pool code, always decrement the active-count counter as the absolute last action in the finally block, after all resource cleanup.

---

## Test

No test added.
