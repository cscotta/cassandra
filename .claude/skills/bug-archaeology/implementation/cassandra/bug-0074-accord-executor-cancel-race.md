# Bug 0074: Race Condition in AccordExecutor cancelExclusive

**Commit**: f3e61c153f · **Ticket**: CASSANDRA-20242
**Subsystem**: service/accord/AccordExecutor.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`CommandStoreQueue` used a plain `Task next` field to track the task currently promoted for execution. When `cancelExclusive` removed a task from the queue it called `CommandStoreQueue.append()`, which assumed `next == null` as a precondition — but a concurrent cancel could clear `next` and then re-invoke `append` while another thread had already set `next`, leading to a lost task or an invalid queue state. The fix separates the "next slot" into a dedicated `CommandStoreQueueTask` wrapper with explicit `isSet()`, `reset()`, and `setNext()` methods, and replaces unconditional `append` calls with `appendOrSetNext`, which atomically decides whether to populate the next slot or fall back to the backing heap queue. Additionally, `AccordTask.addToQueue` was made to silently return when the task is already in `CANCELLED` state, preventing stale tasks from being re-queued after cancellation.

### Before / After

```java
// BEFORE – CommandStoreQueue.append always assumed next == null
void append(Task task) {
    if (next == null) updateNext(task);
    else task.addToQueue(queue);
}
```

```java
// AFTER – atomic check via appendOrSetNext
public void appendOrSetNext(Task task) {
    if (!next.isSet())
        task.addToQueue(this);   // sets next slot
    else
        super.append(task);      // falls back to heap queue
}
```

---

## How to Spot

Any data structure that has a "fast path" field (`next`) alongside a backing collection, where both the fast-path population and removal are done without holding an exclusive lock across the whole check-then-act sequence, is vulnerable to this pattern. During review, look for `if (field == null) set(field); else addToFallback()` in code that can be entered concurrently; the check and set must be atomic or performed exclusively. Similarly, `addToQueue` methods should guard against re-queuing tasks that are already in a terminal state (`CANCELLED`, `FINISHED`).

---

## Test

No test added (covered by simulation tests that exercise cancel paths concurrently).
