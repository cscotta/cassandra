# Bug 1070: Undroppable Messages Discarded When Outbound Queue Is Full

**Commit**: 4c6411f083 · **Ticket**: CASSANDRA-10113
**Subsystem**: src/java/org/apache/cassandra/net/OutboundTcpConnection.java
**Category**: Missing null / bounds check

---

## Root Cause

`OutboundTcpConnection` purges its send queue of expired messages to make room when the queue fills up. The expiry loop iterated over all queued messages and removed those whose timestamp indicated they had exceeded their timeout. However, it did not check the `droppable` flag before removing a message. Certain message types (e.g. migration requests) are marked non-droppable and must never be discarded regardless of queue pressure. Without the guard, non-droppable messages were silently removed from the queue and never delivered, causing operations that relied on them to stall or fail.

### Before / After

```java
// BEFORE — no check for droppable flag; non-droppable messages removed on timeout
while (iter.hasNext())
{
    QueuedMessage qm = iter.next();
    if (qm.timestampNanos >= System.nanoTime() - qm.message.getTimeout())
        return;
    iter.remove();
    // ...
}
```

```java
// AFTER — non-droppable messages are skipped
while (iter.hasNext())
{
    QueuedMessage qm = iter.next();
    if (!qm.droppable)
        continue;
    if (qm.timestampNanos >= System.nanoTime() - qm.message.getTimeout())
        return;
    iter.remove();
    // ...
}
```

---

## How to Spot

Any queue-drain or backpressure loop that removes entries based on age or timeout should be audited for a "must-deliver" (non-droppable) guard. The pattern to look for: an iterator that calls `iter.remove()` after a time comparison, without first verifying that the entry is eligible for removal. Whenever a message type carries a `droppable` or `urgent` flag, every removal site must check it before discarding.

---

## Test

No test added.
