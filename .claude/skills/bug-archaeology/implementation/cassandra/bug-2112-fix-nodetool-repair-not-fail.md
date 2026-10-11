# Bug 2112: nodetool repair exits 0 even when a neighbor node is down

**Commit**: 6bddbb2dd2 · **Ticket**: CASSANDRA-5203
**Subsystem**: src/java/org/apache/cassandra/service/AntiEntropyService.java, src/java/org/apache/cassandra/tools/NodeProbe.java, src/java/org/apache/cassandra/tools/NodeCmd.java
**Category**: Logic error in condition

---

## Root Cause

When `AntiEntropyService` detected that a neighbor endpoint was dead, it logged a message and silently returned instead of propagating the failure. The `RepairRunner` in `NodeProbe` only tracked the `SESSION_FAILED` notification status to detect failures, but the dead-neighbor path never emitted that notification — it just returned. Consequently `NodeCmd` always called `System.exit(0)`, so scripts invoking `nodetool repair` could not detect a failed repair via the process exit code.

### Before / After

```java
// BEFORE (AntiEntropyService.java)
if (!FailureDetector.instance.isAlive(endpoint))
{
    differencingDone.signalAll();
    logger.info(String.format("[repair #%s] Cannot proceed on repair because a neighbor (%s) is dead: session failed", getName(), endpoint));
    return;
}
```

```java
// AFTER (AntiEntropyService.java)
if (!FailureDetector.instance.isAlive(endpoint))
{
    String message = String.format("Cannot proceed on repair because a neighbor (%s) is dead: session failed", endpoint);
    differencingDone.signalAll();
    logger.error(String.format("[repair #%s] ", getName()) + message);
    throw new IOException(message);
}
```

```java
// BEFORE (NodeCmd.java)
System.exit(0);

// AFTER
System.exit(probe.isFailed() ? 1 : 0);
```

---

## How to Spot

When a tool wraps a remote operation and needs to communicate failure to callers, look for code paths that log and return without either throwing or setting a failure flag. Also check that `RepairRunner` (or equivalent listener) handles all failure status codes — missing an enum value means silent success.

---

## Test

No test added.
