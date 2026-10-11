# Bug 1599: Fix Cast Guard to Be of the Cassandra FutureTask Type

**Commit**: f66689db97 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/concurrent/AbstractTracingAwareExecutorService.java
**Category**: Logic error in condition

---

## Root Cause

`AbstractTracingAwareExecutorService.newTaskFor()` contained an `instanceof` check intended to avoid wrapping a `FutureTask` that was already the Cassandra-specific subtype. The check was written against `java.util.concurrent.FutureTask` instead of the local Cassandra `FutureTask` class. Because the Cassandra `FutureTask` extends `java.util.concurrent.FutureTask`, the check would succeed for any `FutureTask`, but the subsequent cast was to the Cassandra `FutureTask<T>`, which would fail at runtime if a plain JDK `FutureTask` was passed—and the tracing-aware wrapping was also skipped for JDK `FutureTask` instances when it should have been applied.

### Before / After

```java
// BEFORE: checks JDK type but casts to Cassandra subtype — wrong guard
if (callable instanceof java.util.concurrent.FutureTask)
    return (FutureTask<T>) callable;
```

```java
// AFTER: checks the Cassandra FutureTask type, consistent with the cast
if (callable instanceof FutureTask)
    return (FutureTask<T>) callable;
```

---

## How to Spot

Any time an `instanceof` check is followed by a cast, the type in the `instanceof` must exactly match (or be the same as) the type in the cast. Mixing a parent-class `instanceof` guard with a subclass cast is a latent `ClassCastException`. A fully-qualified class name in an `instanceof` is a code smell that the author may be checking the wrong type.

---

## Test

No test added.
