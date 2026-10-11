# Bug 1096: Fix Repair Hang When Snapshot Failed

**Commit**: 0a4ce4449f · **Ticket**: CASSANDRA-10057
**Subsystem**: src/java/org/apache/cassandra/repair/RepairMessageVerbHandler.java
**Category**: Resource leak

---

## Root Cause

When `RepairMessageVerbHandler` detected that an already-in-progress repair session overlapped with the sstables just snapshotted, it threw a `RuntimeException` directly from the message handler. Throwing in this path meant the SNAPSHOT response was never sent back to the coordinator. The coordinator had no failure path for this case and waited indefinitely — causing a repair hang. Additionally, the snapshot that was just created was leaked and never cleaned up.

### Before / After

```java
// BEFORE
if (!Sets.intersection(currentlyRepairing, snapshottedSSSTables).isEmpty())
{
    logger.error("Cannot start multiple repair sessions over the same sstables");
    throw new RuntimeException("Cannot start multiple repair sessions over the same sstables");
}
```

```java
// AFTER
if (!Sets.intersection(currentlyRepairing, snapshottedSSSTables).isEmpty())
{
    // clear snapshot that we just created
    cfs.clearSnapshot(desc.sessionId.toString());
    logger.error("Cannot start multiple repair sessions over the same sstables");
    MessageOut reply = new MessageOut(MessagingService.Verb.INTERNAL_RESPONSE)
                           .withParameter(MessagingService.FAILURE_RESPONSE_PARAM, MessagingService.ONE_BYTE);
    MessagingService.instance().sendReply(reply, id, message.from);
    return;
}
```

---

## How to Spot

When a message verb handler encounters an error condition, look for any code path that throws an exception or returns without sending a reply. Repair protocols are driven by request/response pairs; if the coordinator sends a message and receives neither a success nor a failure reply, it will block forever. Any early-exit path in a handler must either send an explicit failure response or be guaranteed to propagate through a higher-level exception handler that does so.

---

## Test

No test added.
