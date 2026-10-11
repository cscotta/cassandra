# Bug 1346: Fix hang when repairing empty keyspace

**Commit**: 8c003a2a5a · **Ticket**: CASSANDRA-8694
**Subsystem**: src/java/org/apache/cassandra/service/ActiveRepairService.java
**Category**: Missing null / bounds check

---

## Root Cause

`ActiveRepairService.submitRepairSession` already guarded against an empty endpoint set by returning `null` early, but it did not guard against an empty `cfnames` array. When a keyspace with no column families was repaired, an empty array was passed in, a `RepairSession` was constructed and submitted, and the session's internal futures never completed because there was nothing to repair — causing the calling code to hang indefinitely waiting on results that would never arrive. Adding a symmetric guard for `cfnames.length == 0` causes the method to return `null` immediately, and callers already know to skip null sessions.

### Before / After

```java
// BEFORE
public RepairFuture submitRepairSession(..., String... cfnames)
{
    RepairSession session = new RepairSession(..., cfnames);
    if (session.endpoints.isEmpty())
        return null;
```

```java
// AFTER
public RepairFuture submitRepairSession(..., String... cfnames)
{
    if (cfnames.length == 0)
        return null;
    RepairSession session = new RepairSession(..., cfnames);
    if (session.endpoints.isEmpty())
        return null;
```

---

## How to Spot

When a method accepts a varargs or array parameter and feeds it to a long-running asynchronous operation, a missing empty-array guard is a classic hang vector. Any time a session, future, or blocking operation is constructed unconditionally from a potentially empty collection, ask: "what happens if the collection is empty?" — particularly when callers already handle a `null` return to mean "nothing to do."

---

## Test

No test added.
