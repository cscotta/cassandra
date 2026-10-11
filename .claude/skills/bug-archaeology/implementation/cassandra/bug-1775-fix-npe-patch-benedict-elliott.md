# Bug 1775: Fix NPE in WaitQueue.signalAll

**Commit**: 5fe0600744 · **Ticket**: CASSANDRA-6671
**Subsystem**: src/java/org/apache/cassandra/utils/concurrent/WaitQueue.java
**Category**: Missing null / bounds check

---

## Root Cause

`WaitQueue.signalAll()` called `queue.getLast()` to capture a snapshot endpoint before iterating, but `ConcurrentLinkedDeque.getLast()` throws `NoSuchElementException` (not returns null) when the queue is empty — which is exactly the state returned by `hasWaiters()` as `false`. Even with the early return, there was a TOCTOU window: the queue could drain between `hasWaiters()` and `getLast()`. The fix replaces the `hasWaiters()` guard with `queue.peekLast()`, which returns `null` on an empty deque and is also safe to use as the snapshot boundary for the subsequent iteration.

### Before / After

```java
// BEFORE – hasWaiters() + getLast() has a TOCTOU gap; getLast() throws on empty deque
if (!hasWaiters())
    return;
// ...
RegisteredSignal last = queue.getLast();
Iterator<RegisteredSignal> iter = queue.iterator();
```

```java
// AFTER – single atomic peek; null means empty
RegisteredSignal last = queue.peekLast();
if (last == null)
    return;
Iterator<RegisteredSignal> iter = queue.iterator();
```

---

## How to Spot

Any two-step pattern of "check if collection is non-empty, then call a throwing accessor" on a concurrent collection contains a TOCTOU race. Prefer `peek`/`poll` variants that return `null` on an empty collection and combine the emptiness check with the element retrieval atomically.

---

## Test

No test added.
