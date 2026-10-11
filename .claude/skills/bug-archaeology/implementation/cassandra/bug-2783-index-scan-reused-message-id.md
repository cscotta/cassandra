# Bug 2783: Index Scan Reuses Same Message Object Across Multiple Endpoints

**Commit**: f51da9a724 · **Ticket**: CASSANDRA-2081

## Detection Rule
When sending the same logical request to multiple endpoints via `MessagingService.sendRR`, call `getMessage()` (or equivalent) inside the loop so each endpoint gets a distinct `Message` object with a unique message ID.

## Code Smell
`Message message = command.getMessage()` is called once before the loop, then `sendRR(message, endpoint, handler)` is called for each endpoint; reusing the same `Message` object shares the message ID across all endpoints.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: reused-message-object-across-endpoints
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
`StorageProxy.getRangeSlice` (index scan path) created a single `Message` instance outside the endpoint loop and sent it to all replicas. Because `MessagingService` maps message IDs to response callbacks, reusing the same ID caused response registration collisions (asserted by the fix's `assert previous == null`) and responses from later endpoints being associated with the first endpoint's callback or dropped entirely. The fix moves `message = command.getMessage()` inside the loop.

## Example
```java
// BEFORE
Message message = command.getMessage(); // one message ID for all
for (InetAddress endpoint : liveEndpoints) {
    MessagingService.instance().sendRR(message, endpoint, handler);
}
```
```java
// AFTER
for (InetAddress endpoint : liveEndpoints) {
    Message message = command.getMessage(); // new ID per endpoint
    MessagingService.instance().sendRR(message, endpoint, handler);
}
```
