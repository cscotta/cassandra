# Bug 0593: Pending Message Count Not Reset After Connection Error in OutboundTcpConnection

**Commit**: 81b6c9e994 · **Ticket**: CASSANDRA-11551
**Subsystem**: `org.apache.cassandra.net`
**Category**: State not cleaned up

---

## Root Cause

`OutboundTcpConnection` maintains a counter `currentMsgBufferCount` that tracks how many messages have been drained from the queue into the current write buffer. When a socket write fails, the code drains and clears both `drainedMessages` and `backlog` to prevent a gossip message buildup, but it forgot to reset `currentMsgBufferCount` to zero. As a result the counter kept accumulating past the number of messages actually pending, so the reported pending message count (visible via metrics) was permanently over-counted after any connection failure.

### Before / After

```java
// BEFORE
drainedMessages.clear();
backlog.clear();
break inner;
```

```java
// AFTER
drainedMessages.clear();
backlog.clear();
currentMsgBufferCount = 0;
break inner;
```

---

## How to Spot

Whenever a data structure is cleared or reset on an error path, audit every derived counter or summary field that was incremented as items were added to that structure — each one must be explicitly zeroed on the same error path. A simple search for `.clear()` calls inside error or exception branches, followed by checking whether all mirrored counters are also reset, will surface this class of bug.

---

## Test

No test added.
