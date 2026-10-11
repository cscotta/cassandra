# Bug 2142: Local Write Timeouts Not Hinted Due to DroppableRunnable Silently Dropping Mutations

**Commit**: de2495c2c9 · **Ticket**: CASSANDRA-4753
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java, net/MessagingService.java
**Category**: State not cleaned up

---

## Root Cause

Local mutation tasks were wrapped in `DroppableRunnable`, which simply drops the task and increments the dropped-message counter when the task exceeds its timeout deadline. Dropped local writes were never converted to hints, so data could be permanently lost on the local node without any compensating hint being stored. The fix introduces `LocalMutationRunnable`, which on timeout re-submits the mutation as a `HintRunnable` rather than silently discarding it. A separate issue was that `MessagingService`'s expired-callback hint path called the internal `scheduleLocalHint` (renamed `submitHint`) which asserted the target was not the local node, but when a local write times out the target is the broadcast address — that assertion would fire. The `LocalMutationRunnable` bypasses this by passing the broadcast address through `HintRunnable` directly.

### Before / After

```java
// BEFORE: local write used DroppableRunnable — timed-out tasks silently dropped
return new DroppableRunnable(MessagingService.Verb.MUTATION)
{
    public void runMayThrow() throws IOException { insertLocal(rm, responseHandler); }
};
// and in MessagingService expired-callback path:
return StorageProxy.scheduleLocalHint(rm, expiredCallbackInfo.target, null, null);
// where scheduleLocalHint asserted: assert !target.equals(FBUtilities.getBroadcastAddress())
```

```java
// AFTER: local write uses LocalMutationRunnable — on timeout, re-submitted as a hint
return new LocalMutationRunnable()
{
    public void runMayThrow() throws IOException { insertLocal(rm, responseHandler); }
};
// LocalMutationRunnable.run() on timeout:
HintRunnable runnable = new HintRunnable(FBUtilities.getBroadcastAddress()) { ... };
submitHint(runnable);
```

---

## How to Spot

Any `DroppableRunnable` used for a local-node mutation should be reviewed: dropping a local write silently means no hint is stored and data is lost. The correct pattern is to convert timed-out local mutations into hints. Also check that hint utility methods correctly handle the local node as a target.

---

## Test

No test added.
