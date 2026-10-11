# Bug 0495: Audit Log and Full Query Logger Incorrectly Coupled via Shared Class Hierarchy

**Commit**: 5242f7d430 · **Ticket**: CASSANDRA-14772
**Subsystem**: src/java/org/apache/cassandra/audit/, src/java/org/apache/cassandra/fql/
**Category**: State not cleaned up

---

## Root Cause

The Full Query Logger (FQL) was implemented as a subclass of `BinLogAuditLogger` and embedded directly inside `AuditLogManager`, meaning enabling or disabling the audit log could inadvertently affect FQL state, and vice versa. The two loggers shared the same code path for registration, lifecycle management, and query interception calls in `QueryProcessor` and the transport message handlers. As a result, toggling one feature could silently suppress events in the other, and disabling audit logging did not cleanly unregister FQL as a listener, leaving stale callbacks. The fix separates them entirely: FQL is moved to its own package, both loggers implement a common `QueryEvents.Listener` interface and register/unregister independently, and `AuditLogManager` no longer owns or controls the FQL lifecycle.

### Before / After

```java
// BEFORE — FQL embedded in AuditLogManager; log() routes to both
private final FullQueryLogger fullQueryLogger;  // owned by AuditLogManager
public void log(AuditLogEntry logEntry) {
    if (isAuditingEnabled()) logAuditLoggerEntry(logEntry);
    if (isFQLEnabled() && fqlIncludeFilter.contains(...)) fullQueryLogger.log(logEntry);
}
```

```java
// AFTER — each logger registers independently as a QueryEvents.Listener
public class AuditLogManager implements QueryEvents.Listener, AuthEvents.Listener { ... }
public class FullQueryLogger implements QueryEvents.Listener { ... }
// Both receive events independently via QueryEvents.instance.notifyXxx()
```

---

## How to Spot

When two orthogonal features (audit logging and query logging) share a class hierarchy or a single manager routes events to both, there is a coupling risk: lifecycle operations on one can silently affect the other. The pattern to watch for is a manager class that both owns an instance of a secondary feature and gates its event delivery through its own enabled/disabled flag. Prefer the observer/listener pattern with independent registration.

---

## Test

`QueryEventsTest` verifies that `QueryEvents` listener notification correctly routes events to registered listeners; `AuditLoggerTest` verifies audit log entries are produced independently of FQL state.
