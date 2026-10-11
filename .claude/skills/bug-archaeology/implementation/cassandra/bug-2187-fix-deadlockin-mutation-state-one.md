# Bug 2187: Deadlock in counter mutation VerbHandler under concurrent CL > ONE writes

**Commit**: 73f479c287 · **Ticket**: CASSANDRA-4578
**Subsystem**: src/java/org/apache/cassandra/db/CounterMutationVerbHandler.java, src/java/org/apache/cassandra/service/AbstractWriteResponseHandler.java, src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Deadlock

---

## Root Cause

`CounterMutationVerbHandler.doVerb` called `StorageProxy.applyCounterMutationOnLeader(...).get()`, blocking the MUTATION stage thread while waiting for acknowledgments from other replicas. Because all replicas run the same handler on the same thread pool, a scenario with CL > ONE and concurrent counter writes could cause every MUTATION thread on every replica to be blocked waiting for each other, resulting in a distributed deadlock. The fix removes the blocking `.get()` call and instead supplies a callback `Runnable` to `applyCounterMutationOnLeader`; the response message is sent asynchronously once the write succeeds.

### Before / After

```java
// BEFORE (blocks the MUTATION stage thread)
StorageProxy.applyCounterMutationOnLeader(cm, localDataCenter).get();
WriteResponse response = new WriteResponse(cm.getTable(), cm.key(), true);
Message responseMessage = WriteResponse.makeWriteResponseMessage(message, response);
MessagingService.instance().sendReply(responseMessage, id, message.getFrom());
```

```java
// AFTER (sends reply asynchronously via callback)
StorageProxy.applyCounterMutationOnLeader(cm, localDataCenter, new Runnable() {
    public void run() {
        WriteResponse response = new WriteResponse(cm.getTable(), cm.key(), true);
        Message responseMessage = WriteResponse.makeWriteResponseMessage(message, response);
        MessagingService.instance().sendReply(responseMessage, id, message.getFrom());
    }
});
```

---

## How to Spot

Calling `.get()` (or any blocking wait) inside a message verb handler is a classic distributed deadlock pattern: the same thread pool that processes incoming requests is blocked waiting for outgoing requests that are themselves handled by the same pool on other nodes. Any handler that coordinates writes across replicas should use callbacks or separate thread pools rather than blocking in-place.

---

## Test

No test added.
