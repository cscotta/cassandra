# Bug 2703: Mutation messages sent multiple times due to misplaced sendMessages call

**Commit**: d9d4176fdf · **Ticket**: CASSANDRA-2557

## Detection Rule
When iterating over per-datacenter message maps to send mutation messages, call `sendMessages()` once after the loop rather than inside the loop body, to avoid sending the accumulated DC messages on every iteration.

## Code Smell
`sendMessages(localDataCenter, dcMessages, responseHandler)` placed inside the `for (InetAddress destination : hintedEndpoints.keySet())` loop, causing the entire `dcMessages` map to be flushed to the messaging service on each iteration instead of once at the end.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: send-inside-accumulation-loop
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
`StorageProxy.sendToHintedEndpoints()` accumulated per-datacenter messages in `dcMessages` inside a loop over all hinted endpoints, then called `sendMessages()` within the same loop body. On each iteration, all previously accumulated DC messages were sent again, resulting in each mutation being delivered multiple times to each destination datacenter. Moving `sendMessages()` to after the loop ensures it is called exactly once.

## Example
```java
// BEFORE (sendMessages inside the for loop)
for (InetAddress destination : hintedEndpoints.keySet()) {
    // ... accumulate into dcMessages ...
    MessagingService.instance().sendOneWay(hintedMessage, destination);
    sendMessages(localDataCenter, dcMessages, responseHandler);  // called per iteration
}
```
```java
// AFTER (sendMessages after the for loop)
for (InetAddress destination : hintedEndpoints.keySet()) {
    // ... accumulate into dcMessages ...
    MessagingService.instance().sendOneWay(hintedMessage, destination);
}
sendMessages(localDataCenter, dcMessages, responseHandler);  // called once
```
