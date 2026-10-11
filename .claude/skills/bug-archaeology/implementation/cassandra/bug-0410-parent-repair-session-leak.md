# Bug 0410: Parent Repair Sessions Leak When Repair Completes Successfully

**Commit**: 23512cf3da · **Ticket**: CASSANDRA-16446
**Subsystem**: src/java/org/apache/cassandra/repair/RepairRunnable.java, src/java/org/apache/cassandra/service/ActiveRepairService.java
**Category**: Resource leak

---

## Root Cause

`ActiveRepairService` maintains a `parentRepairSessions` map that caches session state for each in-progress repair. The session is correctly removed on failure paths via `failRepair`, but was never cleaned up on the success path for normal and preview repairs. Over time, especially on clusters with frequent repairs, this map accumulated entries indefinitely, consuming memory and preventing garbage collection of associated resources.

### Before / After

```java
// BEFORE — success callback had no cleanup call
@Override
public void onSuccess(List<RepairSessionResult> results) {
    // ...
    success("Repair completed successfully");
    executor.shutdownNow();
}
```

```java
// AFTER — cleanup message sent to all participants after success
@Override
public void onSuccess(List<RepairSessionResult> results) {
    // ...
    success("Repair completed successfully");
    ActiveRepairService.instance.cleanUp(parentSession, preparedEndpoints);
    executor.shutdownNow();
}
```

---

## How to Spot

Any time a map or cache is populated at the start of an operation and is supposed to be cleared at the end, verify that every terminal code path—including the success path—performs the cleanup. It is common to handle the failure case via an exception handler but forget the normal-exit branch.

---

## Test

`ActiveRepairServiceMBean` gained a `parentRepairSessionsCount()` method to expose the cache size for monitoring; no in-tree test directly asserts the leak is fixed, but the JMX metric enables operators to observe it.
