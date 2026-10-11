# Bug 2800: Multi-DC Replication Forwards Messages Back to Coordinator Instead of Target

**Commit**: 7d567af62c · **Ticket**: CASSANDRA-2051

## Detection Rule
When forwarding a mutation message to secondary replicas in a multi-DC write path, if the destination address is read from the decoded byte array but the `sendOneWay` call uses `message.getFrom()` (the coordinator's address) instead of the decoded `address`, all forwarded writes go to the coordinator instead of the intended replicas.

## Code Smell
`MessagingService.instance().sendOneWay(message, message.getFrom())` inside the forward loop in `RowMutationVerbHandler.forwardToLocalNodes()` — `message.getFrom()` is the coordinator, not the address being iterated from `forwardBytes`.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: wrong-destination-in-forward-loop
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
In the cross-DC write optimization, a primary DC replica receives a message with embedded forwarding addresses for other local nodes. `forwardToLocalNodes()` decodes each `InetAddress` from the `FORWARD_HEADER` byte array into `address`. However, the `sendOneWay` call mistakenly sent to `message.getFrom()` (the original coordinator) rather than `address` (the decoded forwarding target), so forwarded writes were silently sent back to the coordinator (which already applied the write) instead of the other local replicas. Additionally, the FORWARD_HEADER was being set to `null` with `setHeader()` rather than removed, which could leave a null-valued header entry in the map.

## Example
```java
// BEFORE: sends to coordinator (message.getFrom()), not the decoded address
message.setHeader(RowMutation.FORWARD_HEADER, null); // leaves null in map
// ...decode address from forwardBytes...
MessagingService.instance().sendOneWay(message, message.getFrom()); // WRONG
```
```java
// AFTER: sends to the correct decoded address
message.removeHeader(RowMutation.FORWARD_HEADER); // properly removes key
// ...decode address from forwardBytes...
MessagingService.instance().sendOneWay(message, address); // CORRECT
```
