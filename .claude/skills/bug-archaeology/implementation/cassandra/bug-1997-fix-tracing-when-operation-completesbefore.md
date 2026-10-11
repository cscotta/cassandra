# Bug 1997: Tracing NPE When Operation Completes Before All Responses Arrive

**Commit**: fbe8a6eb21 · **Ticket**: CASSANDRA-5668
**Subsystem**: src/java/org/apache/cassandra/tracing/Tracing.java, src/java/org/apache/cassandra/concurrent/TracingAwareExecutorService.java, src/java/org/apache/cassandra/net/MessagingService.java, src/java/org/apache/cassandra/net/OutboundTcpConnection.java
**Category**: Race condition / missing synchronization

---

## Root Cause

Cassandra's tracing relied on a thread-local `TraceState` that was set when an inbound message arrived and cleared when the coordinating operation finished. If a read operation completed (and cleaned up its trace session) before all replica response messages were processed, the response-handler thread would find `Tracing.instance().get(sessionId)` returning `null` and then call `state.trace(...)` unconditionally, causing a `NullPointerException`. The fix changes the architecture in two ways: (1) `initializeFromMessage()` no longer writes to a thread-local; instead it returns the `TraceState` directly so the executor can inject it explicitly via a new `execute(Runnable, TraceState)` overload; (2) when a response arrives for an already-closed session an `ExpiredTraceState` is returned that writes trace events without a `source_elapsed` value rather than crashing.

### Before / After

```java
// BEFORE – MessagingService.receive()
Tracing.instance().initializeFromMessage(message);  // sets thread-local, may be null session
Tracing.trace("Message received from {}", message.from);  // NPE if session already closed
...
stage.execute(runnable);
```

```java
// AFTER
TraceState state = Tracing.instance().initializeFromMessage(message);  // no thread-local
if (state != null)
    state.trace("Message received from {}", message.from);
...
stage.execute(runnable, state);  // state injected into executor, not thread-local
```

---

## How to Spot

Any pattern of "set thread-local then execute asynchronously" is a race when the session that set the thread-local can be torn down before the async task runs. Prefer passing context explicitly as a parameter through the call chain or injecting it at task-submission time rather than relying on ambient thread-local state that may have been cleared by a concurrent completion path.

---

## Test

`AntiEntropyServiceTestAbstract` was updated to compile against the new `TracingAwareExecutorService` interface; no new behavioural test for the race itself was added.
