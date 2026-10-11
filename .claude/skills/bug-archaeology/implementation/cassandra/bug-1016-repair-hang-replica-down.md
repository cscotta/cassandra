# Bug 1016: Fix incremental repair hang when replica is down

**Commit**: 1538c09214 · **Ticket**: CASSANDRA-10288
**Subsystem**: Repair
**Category**: Race condition / missing synchronization

---

## Root Cause
When a neighbour node was down during incremental repair, `AnticompactionTask.run()` unconditionally sent a messaging request to the dead node without consulting the `FailureDetector`. The request was sent with `sendRR` (request-reply), which waits for a callback that never arrives because the node is unreachable, causing the repair session to hang indefinitely. Similarly, `ActiveRepairService.sendPrepareRequest` sent `PrepareMessage` to down nodes and then waited on a `CountDownLatch` that was never decremented for the unreachable peers.

### Before / After
```java
// BEFORE (AnticompactionTask.run)
AnticompactionRequest acr = new AnticompactionRequest(parentSession);
SemanticVersion peerVersion = SystemKeyspace.getReleaseVersion(neighbor);
if (peerVersion != null && peerVersion.compareTo(VERSION_CHECKER) > 0)
{
    if (doAnticompaction)
        MessagingService.instance().sendRR(acr.createMessage(), neighbor, new AnticompactionCallback(this), ...);
    else
        MessagingService.instance().sendRR(new CleanupMessage(...).createMessage(), neighbor, ...);
}
else
{
    MessagingService.instance().sendOneWay(acr.createMessage(), neighbor);
    set(neighbor);
}

// AFTER (AnticompactionTask.run)
if (FailureDetector.instance.isAlive(neighbor))
{
    // ... same version-branching logic as before ...
}
else
{
    setException(new IOException(neighbor + " is down"));
}
```

---

## How to Spot
Any repair path that issues a `sendRR` (reliable request-reply) toward a peer without first checking `FailureDetector.instance.isAlive()` is at risk. Look for latch-based synchronization (`CountDownLatch`, `AbstractFuture`) where the count-down or future resolution only happens inside message callbacks — if the message is never delivered, the latch never reaches zero. Code review should ask: "what happens if the target is unreachable?" and ensure the failure path always decrements the latch or resolves the future with an exception.

---

## Test
No test added.
