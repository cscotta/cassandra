# Bug 1650: fix build — Replace ConcurrentLinkedQueue with synchronizedList in AbstractRowResolver

**Commit**: dbf3e1c574 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service
**Category**: Race condition / missing synchronization

---

## Root Cause

`AbstractRowResolver.replies` was declared as a `ConcurrentLinkedQueue<>`. A subsequent refactor required iterating over `replies` by index (or using `List`-specific operations), which `ConcurrentLinkedQueue` does not support. The fix replaces the queue with `Collections.synchronizedList(new ArrayList)` while preserving the thread-safety guarantee with a synchronized wrapper. The `java.util.ArrayList` and `java.util.Collections` imports were added, and the field comment was updated to reflect the new implementation.

### Before / After

```java
// BEFORE
// CLQ gives us thread-safety without the overhead of guaranteeing uniqueness like a Set would
protected final Collection<MessageIn<ReadResponse>> replies = new ConcurrentLinkedQueue<>();
```

```java
// AFTER
// synchronizedList gives us thread-safety without the overhead of guaranteeing uniqueness like a Set would
protected final Collection<MessageIn<ReadResponse>> replies =
    Collections.synchronizedList(new ArrayList<MessageIn<ReadResponse>>());
```

---

## How to Spot

Choosing a concurrent collection based on current usage patterns can become a compile or correctness issue when the access pattern changes. Ensure the collection type matches the operations that all consumers (including subclasses) actually need.

---

## Test

No test added.
