# Bug 1708: Fix handling of RejectedExecution in sync Thrift server

**Commit**: 56e2b4ac02 · **Ticket**: CASSANDRA-6788
**Subsystem**: thrift/CustomTThreadPoolServer
**Category**: Race condition / missing synchronization

---

## Root Cause

The sync Thrift server decremented `activeClients` before closing the transport sockets in the worker thread's `finally` block. This meant there was a window where the accept loop could observe a count below the maximum, accept a new connection, and then attempt to submit work to the thread pool — which would throw `RejectedExecutionException` because the old worker thread had decremented the counter but not yet fully exited the pool. The fix moves `activeClients.decrementAndGet()` to after the socket-close calls, closing the race window, and adds an explicit `catch (RejectedExecutionException)` in the accept loop to gracefully handle the residual race with a `continue` rather than a crash. The busy-wait poll interval was also reduced from 100 ms to 10 ms to improve responsiveness.

### Before / After

```java
// BEFORE – worker finally block
finally
{
    activeClients.decrementAndGet();          // decremented first
    if (socket != null)
        ThriftSessionManager.instance.connectionComplete(socket);
}
if (inputTransport != null)  inputTransport.close();   // closed after
if (outputTransport != null) outputTransport.close();
```

```java
// AFTER – worker finally block
finally
{
    if (socket != null)
        ThriftSessionManager.instance.connectionComplete(socket);
    if (inputTransport != null)  inputTransport.close();
    if (outputTransport != null) outputTransport.close();
    activeClients.decrementAndGet();          // decremented last
}
```

---

## How to Spot

When a counter guards resource admission, any decrement should happen only after all resources associated with that slot have been fully released. Decrementing early creates a window where a new requester can be admitted before the previous slot is truly free.

---

## Test

No test added.
