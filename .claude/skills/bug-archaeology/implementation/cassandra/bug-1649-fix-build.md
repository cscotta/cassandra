# Bug 1649: fix build — AbstractRowResolver type narrowed to List

**Commit**: cf3f5b6efb · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service
**Category**: Logic error in condition

---

## Root Cause

After the previous commit (dbf3e1c574) changed `AbstractRowResolver.replies` from `ConcurrentLinkedQueue` to `Collections.synchronizedList(new ArrayList)`, the declared type remained `Collection<MessageIn<ReadResponse>>`. Subclasses that called index-based or list-specific `List` methods on `replies` failed to compile because `Collection` does not expose those methods. The fix narrows the declared type to `List<MessageIn<ReadResponse>>`, which matches the runtime type and exposes the required interface. The `java.util.List` import was also added.

### Before / After

```java
// BEFORE
protected final Collection<MessageIn<ReadResponse>> replies =
    Collections.synchronizedList(new ArrayList<MessageIn<ReadResponse>>());
```

```java
// AFTER
protected final List<MessageIn<ReadResponse>> replies =
    Collections.synchronizedList(new ArrayList<MessageIn<ReadResponse>>());
```

---

## How to Spot

When you change the concrete type returned by a factory/utility method (e.g., `Collections.synchronizedList`), update the declared field type to the most specific interface that callers actually need; using the broadest supertype (`Collection`) hides list-specific operations and causes downstream compile errors.

---

## Test

No test added.
