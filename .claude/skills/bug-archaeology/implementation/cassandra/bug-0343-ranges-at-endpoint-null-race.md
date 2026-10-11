# Bug 0343: Race Condition Returns Null From RangesAtEndpoint Lazy Init

**Commit**: aaf72a7dec · **Ticket**: CASSANDRA-16965
**Subsystem**: src/java/org/apache/cassandra/locator/RangesAtEndpoint.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`RangesAtEndpoint.onlyFull()` and `onlyTransient()` use lazy initialization: they check a nullable field, compute the value if null, assign the field, and return the value. The code read the field twice — once for the null check and once implicitly via the return — without synchronization. A classic double-checked-locking hazard: thread A reads `onlyFull == null`, thread B concurrently assigns `onlyFull = filter(...)`, then thread A assigns `onlyFull = result = filter(...)` and returns `result`. However if the field assignment and the `return` read the field variable rather than the local, a JIT-optimized path could return null. The fix captures the field into a local variable once and uses the local for both the null check and the return, making the pattern safe without requiring volatile or synchronization.

### Before / After

```java
// BEFORE – reads field twice; races can return null
public RangesAtEndpoint onlyFull()
{
    RangesAtEndpoint result = onlyFull;
    if (onlyFull == null)           // reads field again
        onlyFull = result = filter(Replica::isFull);
    return result;
}
```

```java
// AFTER – single read of field into local; null check uses local
public RangesAtEndpoint onlyFull()
{
    RangesAtEndpoint result = onlyFull;
    if (result == null)             // uses local copy
        onlyFull = result = filter(Replica::isFull);
    return result;
}
```

---

## How to Spot

In any lazy-initialization pattern, the field should be read exactly once into a local variable before the null check; all subsequent uses of the value should reference that local, not re-read the field. Re-reading the field after the null check opens a window for another thread to set it between the check and the read, which can result in returning null even when the field has been set. Review all `if (field == null) field = ...; return field;` patterns and replace the final `return field` with a `return local` that was captured before the check.

---

## Test

No test added; the fix is a one-line change to the lazy-init pattern.
