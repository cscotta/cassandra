# Bug 2262: Tracing Appender Records Wrong Thread Name

**Commit**: e49d140bb7 · **Ticket**: CASSANDRA-4599
**Subsystem**: src/java/org/apache/cassandra/tracing/TracingAppender.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`TracingAppender.append()` captured the log event and then submitted an asynchronous `Runnable` to the tracing stage. Inside that `Runnable`, `event.getThreadName()` was called to record which thread produced the log event. However, the `LoggingEvent` object can be reused or recycled by the logging framework after `append()` returns, so by the time the tracing stage executes the runnable, the thread name stored in the event may have changed or become stale. The fix captures `event.getThreadName()` eagerly in the `append()` method body (before the runnable is submitted) and closes over the captured string value, ensuring the correct originating thread name is always recorded.

### Before / After

```java
// BEFORE – thread name read lazily inside async task, may be stale
StageManager.getStage(Stage.TRACING).execute(new WrappedRunnable()
{
    public void runMayThrow() ...
    {
        ...
        addColumn(cf, buildName(cfMeta, eventId, bytes("thread")), event.getThreadName());
```

```java
// AFTER – thread name captured before the async handoff
final String threadName = event.getThreadName();
StageManager.getStage(Stage.TRACING).execute(new WrappedRunnable()
{
    public void runMayThrow() ...
    {
        ...
        addColumn(cf, buildName(cfMeta, eventId, bytes("thread")), threadName);
```

---

## How to Spot

Whenever a mutable or reusable object (logging event, buffer, request context) is handed off to an asynchronous task, all fields that need to be read should be extracted into `final` locals before the handoff. Any read of such an object inside a lambda or anonymous `Runnable` that crosses a thread boundary is a potential race condition.

---

## Test

No test added.
