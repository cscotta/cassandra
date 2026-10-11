# Bug 1540: Fix Tracing of Concurrent Range Slices

**Commit**: 948ae016f4 · **Ticket**: CASSANDRA-7626
**Subsystem**: src/java/org/apache/cassandra/tracing
**Category**: Race condition / missing synchronization

---

## Root Cause

When multiple concurrent range-slice sub-requests shared the same `TraceState` (one per coordinator session), `Tracing.stopNonLocal()` would remove the session from the sessions map as soon as the *first* response arrived, causing all subsequent responses to fail to find the `TraceState` and silently drop their trace events. There was no reference counting to track how many in-flight requests were using the same session.

### Before / After

```java
// BEFORE — first response removes shared session immediately
public void stopNonLocal(TraceState state)
{
    sessions.remove(state.sessionId);
}

// initiate() lookup: no acquire
TraceState ts = sessions.get(sessionId);
if (ts != null)
    return ts;
```

```java
// AFTER — reference-counted; only last responder removes session
private final AtomicInteger references = new AtomicInteger(1);

public boolean acquireReference() { /* CAS loop */ }
public int releaseReference() { return references.decrementAndGet(); }

public void doneWithNonLocalSession(TraceState state)
{
    if (state.releaseReference() == 0)
        sessions.remove(state.sessionId);
}

// initiate() lookup: acquire before returning
if (ts != null && ts.acquireReference())
    return ts;
```

---

## How to Spot

Any shared mutable object that has a teardown method (remove, close, stop) must use reference counting when multiple concurrent callers can hold references. A single unconditional remove on a shared session object is a strong signal that concurrent access was not considered.

---

## Test

No test added.
