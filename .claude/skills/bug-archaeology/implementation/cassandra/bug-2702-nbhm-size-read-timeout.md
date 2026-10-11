# Bug 2702: NBHM.size() unreliable in ReadCallback causes read timeouts

**Commit**: 5e6539addb · **Ticket**: CASSANDRA-2552

## Detection Rule
When counting received responses in a concurrent callback, use an `AtomicInteger` counter that is incremented on each `response()` call, rather than calling `resolver.getMessageCount()` which delegates to a non-blocking hash map's `size()` — a method that can return stale values under concurrent mutation.

## Code Smell
`if (repairResolver.getMessageCount() == count)` where `getMessageCount()` calls `replies.size()` on a `NonBlockingHashMap` whose `size()` is not guaranteed to be accurate after concurrent `put()` operations.

## Tags
- scope: java
- category: race-condition
- code-pattern: nonblocking-map-size-unreliable
- severity: hang
- subsystem: network-messaging

## Root Cause
`AsyncRepairCallback.response()` checked `repairResolver.getMessageCount() == count` to decide when all expected replies had arrived. `NBHM.size()` is O(n) and can return an inconsistent value under concurrent insertions, causing the equality check to be missed and the read-repair callback to never fire — effectively making reads time out even though all responses were received. The fix replaces `getMessageCount()` with an `AtomicInteger received` that is incremented atomically in `response()`, making the completion check reliable.

## Example
```java
// BEFORE
public void response(Message message) {
    repairResolver.preprocess(message);
    if (repairResolver.getMessageCount() == count) { ... trigger repair ... }
}
```
```java
// AFTER
protected final AtomicInteger received = new AtomicInteger(0);
public void response(Message message) {
    repairResolver.preprocess(message);
    if (received.incrementAndGet() == blockfor) { ... trigger repair ... }
}
```
