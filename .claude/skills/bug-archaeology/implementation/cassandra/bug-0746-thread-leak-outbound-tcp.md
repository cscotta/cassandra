# Bug 0746: Thread leak in OutboundTcpConnection on connection failure

**Commit**: a6237bf65a · **Ticket**: CASSANDRA-13204
**Subsystem**: Networking (internode messaging)
**Category**: Resource leak

---

## Root Cause
`OutboundTcpConnection.closeSocket()` cleared the backlog *before* setting `isStopped`, which created a window where the run loop could re-enter the drain/send path and re-populate the backlog with new messages arriving concurrently. Additionally, the outer `while(true)` loop in `run()` never checked `isStopped`, so even after a close was initiated the thread would spin indefinitely trying to connect. When `connect()` returned `false` (i.e., no socket could be established), the loop drained the backlog but did not clear the local `drainedMessages` list before continuing, causing the same stale messages to be retried. Together these issues left the connection thread alive and consuming resources after the connection was meant to be torn down.

### Before / After
```java
// BEFORE — clear happens before isStopped is set; outer loop never checks isStopped
void closeSocket(boolean destroyThread) {
    backlog.clear();
    isStopped = destroyThread;
    enqueue(CLOSE_SENTINEL, -1);
}
// outer: while (true) { ... }

// AFTER — isStopped set first; loop exits when flag is true; connect failure clears local list
void closeSocket(boolean destroyThread) {
    isStopped = destroyThread;
    backlog.clear();
    enqueue(CLOSE_SENTINEL, -1);  // unblocks backlog.take() in run()
}
// outer: while (!isStopped) { ...
//   if connect() fails:
//     drainedMessages.clear();
//     backlog.clear();
//     break inner;
```

---

## How to Spot
In connection-management threads, the stop flag must be visible to the run loop before side effects like clearing the queue take place. A `while(true)` loop that reads a stop flag only at an inner break rather than the outer condition is a classic thread-leak pattern. Whenever a failed-connection branch clears a shared queue, the companion in-flight local list must also be cleared to avoid replaying the same messages on the next iteration.

---

## Test
No new test added; the fix was validated by existing messaging tests.
