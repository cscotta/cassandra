# Bug 1896: CME in StreamResultFuture Event Listener List

**Commit**: 6529d7309a · **Ticket**: CASSANDRA-6129
**Subsystem**: src/java/org/apache/cassandra/streaming
**Category**: Race condition / missing synchronization

---

## Root Cause

`StreamResultFuture` stored its event listeners in a `Collections.synchronizedList(new ArrayList<>())`. While individual method calls on that wrapper are synchronized, iterating the list requires external synchronization that was not present — concurrent additions during iteration cause a `ConcurrentModificationException`. Replacing the list with a `ConcurrentLinkedQueue` allows safe concurrent reads and writes without external synchronization, eliminating the CME.

### Before / After

```java
// BEFORE
private final List<StreamEventHandler> eventListeners =
    Collections.synchronizedList(new ArrayList<StreamEventHandler>());
```

```java
// AFTER
private final Collection<StreamEventHandler> eventListeners =
    new ConcurrentLinkedQueue<>();
```

---

## How to Spot

When a collection is both iterated and modified from multiple threads, `synchronizedList` is insufficient unless every iteration site is wrapped in a `synchronized (list)` block. Prefer `ConcurrentLinkedQueue` or `CopyOnWriteArrayList` for listener/callback lists that are traversed while new entries may still be added.

---

## Test

No test added.
