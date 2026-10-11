# Bug 1269: Fix Threadpool in RepairSession Not Shutdown on Failure

**Commit**: 7223ec025b · **Ticket**: CASSANDRA-9260
**Subsystem**: src/java/org/apache/cassandra/repair/RepairSession.java
**Category**: Resource leak

---

## Root Cause

`RepairSession` manages an internal executor/thread-pool to coordinate repair jobs. When the session completed with an exception, the error handler called `setException(t)` directly on the underlying `AbstractFuture`, which recorded the failure and notified listeners but did not trigger the shutdown sequence for the session's thread-pool. The correct method to call on failure is `forceShutdown(t)`, which both records the exception and performs the orderly teardown. As a result, a failed repair session leaked its thread-pool indefinitely.

### Before / After

```java
// BEFORE
setException(t);
```

```java
// AFTER
forceShutdown(t);
```

---

## How to Spot

When a class wraps a resource (thread pool, connection, file handle) and exposes both a "record error" path and a "shutdown + record error" path, verify that the failure handler always calls the latter. `setException`/`setResult` on a Future base class is rarely the right call in a resource-owning subclass; look for a `shutdown`, `close`, or `forceShutdown` wrapper.

---

## Test

No test added.
