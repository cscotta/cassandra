# Bug 2486/2487: Mutable message headers shared across threads — race in multi-DC write forwarding

**Commit**: 789aa15fc1 (1.0.x) / d655e7868f (0.8.x) · **Ticket**: CASSANDRA-3530

## Detection Rule

When a single mutable `Message` object is passed to multiple threads (e.g., forwarded to several nodes in a loop), and each send path mutates the message's header map in-place, the mutations race against each other and against the serialization happening on the network thread.

## Code Smell

`message.removeHeader(key)` / `message.setHeader(key, value)` mutating shared mutable state on a `Message` instance that is reused across loop iterations or shared between threads. The `Header.details_` field is a `Hashtable` (mutable) assigned by reference.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: shared-mutable-message-header
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause

`Message` and `Header` objects used a mutable `Hashtable` for per-message metadata. When forwarding writes in multi-DC setups, the same `Message` object was shared between the forwarding loop iteration and the background network I/O thread. `removeHeader` and `setHeader` mutated the shared map in-place. The fix makes `Header.details_` an `ImmutableMap` and changes all mutations to return new `Message`/`Header` copies (`withHeaderAdded`, `withHeaderRemoved`), making message objects effectively immutable after construction.

## Example

```java
// BEFORE
public void removeHeader(String key) {
    header_.removeDetail(key);  // mutates shared mutable map
}
// In forwarding:
message.removeHeader(RowMutation.FORWARD_HEADER);
MessagingService.instance().sendOneWay(message, address);
```

```java
// AFTER
public Message withHeaderRemoved(String key) {
    return new Message(header_.withDetailsRemoved(key), body_, version);  // new copy
}
// In forwarding:
Message messageCopy = message.withHeaderRemoved(RowMutation.FORWARD_HEADER);
MessagingService.instance().sendOneWay(messageCopy, address);
```
