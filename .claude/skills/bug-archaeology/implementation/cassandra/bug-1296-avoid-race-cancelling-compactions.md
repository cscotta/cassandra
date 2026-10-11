# Bug 1296: Avoid Race in Cancelling Compactions

**Commit**: 2cf4ca3a94 · **Ticket**: CASSANDRA-9070
**Subsystem**: db/compaction/CompactionTask.java
**Category**: Race condition / missing synchronization

---

## Root Cause

When a compaction strategy is disabled (e.g. during a schema change or node decommission), running `CompactionTask` instances were not notified promptly. The task would proceed past the point where it checks for pending work even though the strategy had already been marked inactive, leading to wasted work or undefined behavior. The fix adds an early check for `isActive` immediately after the compaction info is registered with the metrics collector, ensuring the task is aborted before any actual I/O is done.

### Before / After

```java
// BEFORE
collector.beginCompaction(ci);
try
{
    if (!iter.hasNext())
    { ...
```

```java
// AFTER
collector.beginCompaction(ci);
try
{
    if (!controller.cfs.getCompactionStrategy().isActive)
        throw new CompactionInterruptedException(ci.getCompactionInfo());

    if (!iter.hasNext())
    { ...
```

---

## How to Spot

Whenever a long-running task operates under a strategy or policy object that can be asynchronously invalidated, look for a window between the moment the task starts and the moment it first consults that strategy. If the task does not re-check liveness before performing work, a concurrent disable or replacement of the strategy can leave the task running against a defunct context. The pattern to look for is: acquire resource, register with metrics, then proceed without checking whether the owning subsystem is still active.

---

## Test

No test added.
