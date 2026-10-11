# Bug 1392: Nodetool netstats Throws Undeclared Exception When Node Is Starting

**Commit**: 6424871439 · **Ticket**: CASSANDRA-8122
**Subsystem**: src/java/org/apache/cassandra/tools/NodeTool.java
**Category**: Missing null / bounds check

---

## Root Cause

When `nodetool netstats` is invoked against a node that is still in the `STARTING` operation mode, the `MessagingService` and its JMX proxies are not yet fully initialized. Attempting to call `getCommandPendingTasks()`, `getResponsePendingTasks()`, and the read-repair stats through uninitialized JMX beans causes an undeclared throwable exception. The code assumed the node was always in a queryable state, with no guard against the transient startup window.

### Before / After

```java
// BEFORE
System.out.printf("Read Repair Statistics:...", probe.getReadRepairAttempted(), ...);
MessagingServiceMBean ms = probe.msProxy;
// ... iterates ms.getCommandPendingTasks() unconditionally
```

```java
// AFTER
if (!probe.isStarting())
{
    System.out.printf("Read Repair Statistics:...", probe.getReadRepairAttempted(), ...);
    MessagingServiceMBean ms = probe.msProxy;
    // ... iterates ms.getCommandPendingTasks() only when node is ready
}
```

---

## How to Spot

Any nodetool command that reads JMX state from subsystems initialized after the node's startup phase should guard against the `STARTING` mode. Look for code that dereferences JMX proxy objects (`msProxy`, `ssProxy`, etc.) without first checking `probe.isStarting()` or the equivalent `getOperationMode()` string comparison — these are latent crash sites whenever the tool is run during the startup window.

---

## Test

No test added.
