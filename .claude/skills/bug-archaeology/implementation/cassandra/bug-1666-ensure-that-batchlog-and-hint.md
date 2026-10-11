# Bug 1666: Ensure That Batchlog and Hint Timeouts Do Not Produce High Dropped Message Rates

**Commit**: 2890cc5be9 · **Ticket**: CASSANDRA-7058
**Subsystem**: src/java/org/apache/cassandra/db/ (BatchlogManager, HintedHandOffManager, MessagingService)
**Category**: Wrong constant / default value

---

## Root Cause

`BatchlogManager` and `HintedHandOffManager` sent replay/delivery mutations via the standard `sendRR` path, which registers a callback that generates a hint on timeout. This meant that a timed-out batchlog replay or hint delivery would itself trigger a new hint, causing an infinite cascade: each failed delivery produced another hint entry and inflated the "dropped messages" counter. The fix introduces a dedicated `sendUnhintableMutation` path that registers the callback without enabling the hint-on-timeout behaviour.

### Before / After

```java
// BEFORE – uses sendRR which can trigger a hint on timeout
MessagingService.instance().sendRR(mutation.createMessage(), ep, handler);
```

```java
// AFTER – uses a path that suppresses hint generation on timeout
MessagingService.instance().sendUnhintableMutation(mutation, ep, handler);
```

---

## How to Spot

Any subsystem that is itself responsible for delivering hints or replaying batches must not use the general write path that can produce new hints on failure. Whenever `sendRR` is called with a write mutation, ask: "if this times out, should a hint be created?" — for hint delivery and batchlog replay the answer is always no.

---

## Test

No test added.
