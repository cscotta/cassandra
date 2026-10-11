# Bug 1597: Fix Handling of Empty Counter Replication Mutations

**Commit**: 1dc157afe7 · **Ticket**: CASSANDRA-7144
**Subsystem**: src/java/org/apache/cassandra/db/CounterMutation.java, src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Missing null / bounds check

---

## Root Cause

`CounterMutation.makeReplicationMutation()` could produce an empty `RowMutation` when the replicated counter columns had all been filtered out (e.g., because none of the remote replicas held any of the updated counter cells). The `StorageProxy` then sent this empty mutation to remote nodes without checking, causing those nodes to receive and acknowledge an empty write, which could still stall the coordinator waiting for responses—or produce unexpected behavior on the replica side. The fix makes `makeReplicationMutation()` return `null` for empty mutations and teaches `StorageProxy` to simulate acknowledgements rather than dispatching a no-op write.

### Before / After

```java
// BEFORE: always returns a mutation, even if empty
return replicationMutation;
```

```java
// AFTER: returns null when mutation is empty
return replicationMutation.isEmpty() ? null : replicationMutation;
```

```java
// AFTER: StorageProxy simulates responses for null replication mutation
RowMutation replicationMutation = cm.makeReplicationMutation();
if (replicationMutation != null)
{
    sendToHintedEndpoints(cm.makeReplicationMutation(), remotes, responseHandler, ...);
}
else
{
    // simulate the rest of the responses to avoid the timeout
    for (int i = 0; i < remotes.size(); i++)
        responseHandler.response(null);
}
```

---

## How to Spot

When a method builds and returns a collection or mutation object, review callers to ensure they handle the empty case. Returning an empty mutable object rather than null is idiomatic Java, but callers that send it over the network or store it must be prepared for the no-op case and not block waiting for responses that may never come.

---

## Test

No test added.
