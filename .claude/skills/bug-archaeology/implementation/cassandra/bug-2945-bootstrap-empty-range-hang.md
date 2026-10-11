# Bug 2945: Bootstrap hangs when source node has no data in requested range

**Commit**: e734d837a0 · **Ticket**: CASSANDRA-541

## Detection Rule
When a message is sent to initiate a streaming transfer and then the code unconditionally waits for stream completion, check whether the transfer may have zero files — which means the wait will never be satisfied.

## Code Smell
`transferOneTable` sends a `StreamInitiateMessage` and then calls `StreamManager.instance(target).waitForStreamCompletion()` unconditionally; if `streamContexts.length == 0` the receiver returns early without signalling completion, causing an infinite hang.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: unconditional-wait-on-empty-transfer
- severity: hang
- subsystem: streaming

## Root Cause
When a bootstrap source node has no data for the requested token range, `sstables` is empty, producing a zero-length `streamContexts` array. The message was still sent, but the receiver had no work to complete and therefore never sent a "done" signal. The sender blocked forever in `waitForStreamCompletion`. Two fixes are needed: (1) the sender must skip the wait when contexts are empty, and (2) the receiver must detect a zero-context initiate message and call `removeBootstrapSource` to unblock bootstrap.

## Example
```java
// BEFORE (sender always waits)
MessagingService.instance().sendOneWay(message, target);
if (logger.isDebugEnabled())
    logger.debug("Waiting for transfer to " + target + " to complete");
StreamManager.instance(target).waitForStreamCompletion();
```
```java
// AFTER (sender only waits when there are files)
MessagingService.instance().sendOneWay(message, target);
if (streamContexts.length > 0) {
    StreamManager.instance(target).waitForStreamCompletion();
}
// receiver side: return early and remove bootstrap source when 0 contexts
if (streamContexts.length == 0 && StorageService.instance().isBootstrapMode()) {
    StorageService.instance().removeBootstrapSource(message.getFrom());
    return;
}
```
