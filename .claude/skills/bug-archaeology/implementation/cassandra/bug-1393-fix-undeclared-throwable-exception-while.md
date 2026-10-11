# Bug 1393: Nodetool netstats Throws Undeclared Exception When Node Is Starting (Legacy NodeCmd)

**Commit**: 54b4b99e1e · **Ticket**: CASSANDRA-8122
**Subsystem**: src/java/org/apache/cassandra/tools/NodeCmd.java
**Category**: Missing null / bounds check

---

## Root Cause

This is the companion fix to CASSANDRA-8122 applied to the older `NodeCmd`-based nodetool path. The same pattern existed: `printNetStats()` in `NodeCmd` accessed `MessagingServiceMBean` and read-repair statistics unconditionally, without checking whether the node was still in the `STARTING` mode and the backing MBeans were not yet registered. The fix required adding `isStarting()` to both `StorageServiceMBean` interface and `StorageService` implementation, and a corresponding delegation method in `NodeProbe`, so the guard could be applied uniformly across both CLI paths.

### Before / After

```java
// BEFORE (NodeCmd.printNetStats)
outs.printf("Read Repair Statistics:...", probe.getReadRepairAttempted(), ...);
MessagingServiceMBean ms = probe.msProxy;
for (int n : ms.getCommandPendingTasks().values()) ...
```

```java
// AFTER (NodeCmd.printNetStats)
if (!probe.isStarting())
{
    outs.printf("Read Repair Statistics:...", probe.getReadRepairAttempted(), ...);
    MessagingServiceMBean ms = probe.msProxy;
    for (int n : ms.getCommandPendingTasks().values()) ...
}
```

---

## How to Spot

When the same operation-mode guard needs to be added in two parallel CLI implementations (`NodeTool` and `NodeCmd`), it is easy to miss one. A reviewer should search for all call sites that touch JMX proxies without first checking node state and verify that every code path (old and new API) has been updated consistently. The absence of `isStarting()` from the `StorageServiceMBean` interface was itself a signal that the abstraction was incomplete.

---

## Test

No test added.
