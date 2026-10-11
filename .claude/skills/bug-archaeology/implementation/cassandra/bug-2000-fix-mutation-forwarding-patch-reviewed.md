# Bug 2000: Cross-DC Mutation Forwarding Sent Stale Message With Leftover FORWARD_TO Header

**Commit**: b1d7405fd1 · **Ticket**: CASSANDRA-5632
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java, src/java/org/apache/cassandra/db/RowMutationVerbHandler.java
**Category**: State not cleaned up

---

## Root Cause

When forwarding mutations to remote datacenters, a single `MessageOut` object was reused across multiple DC groups via a `Multimap<MessageOut, InetAddress>`. To avoid infinite forwarding loops, `withHeaderRemoved(RowMutation.FORWARD_TO)` was called to strip the forwarding header before each send — but the old design still grouped targets under the same message key, causing the cleanup to be unreliable. The fix restructures the code so that one fresh message is created per remote DC batch, with the `FORWARD_FROM` parameter correctly stamped on it, and removes the `withHeaderRemoved` method entirely.

Additionally, in `RowMutationVerbHandler`, `replyTo` was unconditionally set to `message.from` before checking whether a `FORWARD_FROM` header was present, meaning the ACK could be sent to the wrong coordinator.

### Before / After

```java
// BEFORE: message object shared across DC groups, header stripped manually
Map<String, Multimap<MessageOut, InetAddress>> dcMessages = new HashMap<>();
// ... then per DC:
message = message.withHeaderRemoved(RowMutation.FORWARD_TO);
sendMessagesToOneDC(message, targets, isLocalDC, handler);
```

```java
// AFTER: one message created once, distributed per DC group
MessageOut<RowMutation> message = rm.createMessage();
for (Map.Entry<String, Collection<InetAddress>> entry : dcGroups.entrySet()) {
    sendMessagesToOneDC(message, entry.getValue(), isLocalDC, responseHandler);
}
```

---

## How to Spot

When a message object is mutated (or requires cleanup) across loop iterations that share it, check that the mutation is truly safe to share. Any pattern of "remove header before send in a loop" is a red flag — prefer creating a clean message per logical group instead.

---

## Test

No test added.
