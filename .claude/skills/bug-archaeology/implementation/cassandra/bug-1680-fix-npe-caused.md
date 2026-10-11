# Bug 1680: Fix NPE Caused by CASSANDRA-6818

**Commit**: c6ec5d5b2b · **Ticket**: CASSANDRA-6984
**Subsystem**: src/java/org/apache/cassandra/streaming
**Category**: Missing null / bounds check

---

## Root Cause

When a stream transfer task has already been completed or aborted, calling `transfers.get(header.cfId)` in `StreamSession.fileSent()` returns `null`, and the code unconditionally called `.scheduleTimeout()` on that null reference. Additionally, the message reader loop in `ConnectionHandler` only caught `IOException`, so any non-IO `Throwable` (such as the NPE itself) propagated out of the loop without notifying the session of the error.

### Before / After

```java
// BEFORE
transfers.get(header.cfId).scheduleTimeout(header.sequenceNumber, 12, TimeUnit.HOURS);
```

```java
// AFTER
StreamTransferTask task = transfers.get(header.cfId);
if (task != null)
{
    task.scheduleTimeout(header.sequenceNumber, 12, TimeUnit.HOURS);
}
```

```java
// BEFORE (ConnectionHandler message loop)
catch (IOException e)
{
    session.onError(e);
}
```

```java
// AFTER
catch (Throwable e)
{
    session.onError(e);
}
```

---

## How to Spot

Any direct method call on the result of a `Map.get()` without a null guard is a candidate NPE. When the map key is a dynamic value (a UUID-based cfId) and entries are removed during concurrent operations, the lookup can return null even if the entry was present moments before.

---

## Test

No test added.
