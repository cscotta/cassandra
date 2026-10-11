# Bug 0571: Read Repair Sends Self-Directed Query Through Messaging Service

**Commit**: f24e23c5f4 · **Ticket**: CASSANDRA-14807
**Subsystem**: `src/java/org/apache/cassandra/service/reads/repair/AbstractReadRepair.java`
**Category**: Logic error in condition

---

## Root Cause

`AbstractReadRepair.sendReadCommand` was unconditionally routing all read-repair sub-queries through `MessagingService`, including queries addressed to the local node itself. Sending a message to self via the messaging service is unnecessary overhead and can trigger serialization/deserialization paths or deadlocks that are avoided when executing locally. The fix adds an early check: if the target replica is the local node, the command is dispatched directly onto the `READ` stage thread pool via `StorageProxy.LocalReadRunnable`, bypassing the network stack entirely.

### Before / After

```java
// BEFORE
void sendReadCommand(Replica to, ReadCallback readCallback, boolean speculative)
{
    ReadCommand command = this.command;
    if (to.isTransient())
    { ...
```

```java
// AFTER
void sendReadCommand(Replica to, ReadCallback readCallback, boolean speculative)
{
    ReadCommand command = this.command;
    if (to.isSelf())
    {
        StageManager.getStage(Stage.READ).maybeExecuteImmediately(
            new StorageProxy.LocalReadRunnable(command, readCallback));
        return;
    }
    if (to.isTransient())
    { ...
```

---

## How to Spot

Any code path that iterates over a replica set and sends a message to each should include a self-check before dispatching through the messaging layer. Search for patterns where `MessagingService.send*` or similar is called in a loop over replicas without a preceding `isSelf()` guard. The local-execution fast path is well-established in `StorageProxy`; read-repair code that was refactored away from `StorageProxy` can lose that guard if it is not explicitly re-added.

---

## Test

No test added.
