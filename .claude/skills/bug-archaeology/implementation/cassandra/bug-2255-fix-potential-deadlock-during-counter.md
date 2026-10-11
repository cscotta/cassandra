# Bug 2255: Deadlock During Counter Writes When Replica Blocks on Its Own Response Handler

**Commit**: 7371e10be4 · **Ticket**: CASSANDRA-4578
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java, CounterMutationVerbHandler.java, AbstractWriteResponseHandler.java, WriteResponseHandler.java, DatacenterWriteResponseHandler.java, DatacenterSyncWriteResponseHandler.java, AbstractReplicationStrategy.java
**Category**: Deadlock

---

## Root Cause

Counter writes in Cassandra use a two-phase protocol: the coordinator forwards the mutation to a designated leader replica, which applies it locally and waits for quorum acknowledgements from other replicas before responding to the coordinator. The `CounterMutationVerbHandler` on the leader replica called `applyCounterMutationOnLeader(...).get()` — blocking the MUTATION stage thread until the full quorum was acknowledged. If enough replicas were themselves tied up in MUTATION stage threads waiting on the same or other counter mutations, no thread was available to send responses, causing a distributed deadlock.

The fix removes the blocking `.get()` call from `CounterMutationVerbHandler` and instead passes a `Runnable` callback to `applyCounterMutationOnLeader`. The callback sends the reply to the coordinator only after the quorum condition is met (signalled inside `AbstractWriteResponseHandler.signal()`), freeing the MUTATION thread immediately. All `WriteResponseHandler` variants and `AbstractReplicationStrategy.getWriteResponseHandler` were updated to thread the optional callback parameter through the hierarchy.

### Before / After

```java
// BEFORE (CounterMutationVerbHandler)
StorageProxy.applyCounterMutationOnLeader(cm, localDataCenter).get(); // blocks
WriteResponse response = new WriteResponse(...);
MessagingService.instance().sendReply(responseMessage, id, message.getFrom());

// BEFORE (AbstractWriteResponseHandler)
// condition.signal() called directly; no callback mechanism
```

```java
// AFTER (CounterMutationVerbHandler)
StorageProxy.applyCounterMutationOnLeader(cm, localDataCenter, new Runnable() {
    public void run() {
        // send reply asynchronously once quorum met
        MessagingService.instance().sendReply(...);
    }
});  // returns immediately; no blocking

// AFTER (AbstractWriteResponseHandler)
protected void signal() {
    condition.signal();
    if (callback != null) callback.run();
}
```

---

## How to Spot

When a thread that handles incoming RPC messages blocks waiting for outgoing RPC responses, look for potential deadlock if the responses can only be processed by the same thread pool. The general fix is to replace blocking `.get()` with a completion callback that executes the continuation asynchronously after the condition is satisfied.

---

## Test

No test added.
