# Bug 1858: Socket Leak in Connection Pool Creation Under Concurrent Access

**Commit**: 8c2404493a · **Ticket**: CASSANDRA-6308
**Subsystem**: net (MessagingService)
**Category**: Resource leak

---

## Root Cause

`OutboundTcpConnectionPool` opens a socket upon construction. The original code created the pool object and then called `putIfAbsent`, discarding the return value. If two threads raced to create a pool for the same address, the loser's `putIfAbsent` would silently return the winning entry, but the already-opened socket in the loser's pool object was never closed — leaking a file descriptor and an established TCP connection.

### Before / After

```java
// BEFORE
connectionManagers.putIfAbsent(to, new OutboundTcpConnectionPool(to));
cp = connectionManagers.get(to);
```

```java
// AFTER
cp = new OutboundTcpConnectionPool(to);
OutboundTcpConnectionPool existingPool = connectionManagers.putIfAbsent(to, cp);
if (existingPool != null)
{
    cp.close();
    cp = existingPool;
}
```

---

## How to Spot

Any time an object that holds a resource (socket, file handle, thread) is constructed before a `putIfAbsent` or `computeIfAbsent` call, check that the losing race participant disposes of the resource on conflict. The type signature of `putIfAbsent` returning the previous value is the signal — that return value must never be ignored.

---

## Test

No test added.
