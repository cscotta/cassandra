# Bug 0680: NPE in Bootstrap Error Handler When Exception Has No Cause

**Commit**: 5b982d790b · **Ticket**: CASSANDRA-13272
**Subsystem**: `src/java/org/apache/cassandra/service/StorageService.java`
**Category**: Missing null / bounds check

---

## Root Cause

The `onFailure` callback inside `StorageService.resumeBootstrap` unconditionally called `e.getCause().getMessage()` without checking whether `getCause()` returns null. A `ListenableFuture` failure can wrap the original exception directly as an `ExecutionException`, but it can also deliver exceptions that are not `ExecutionException` wrappers, so `getCause()` can legitimately be null. The developer assumed every thrown exception would be wrapped, missing the case where the exception itself carries the message.

### Before / After

```java
// BEFORE
public void onFailure(Throwable e)
{
    String message = "Error during bootstrap: " + e.getCause().getMessage();
    logger.error(message, e.getCause());
    ...
}
```

```java
// AFTER
public void onFailure(Throwable e)
{
    String message = "Error during bootstrap: ";
    if (e instanceof ExecutionException && e.getCause() != null)
        message += e.getCause().getMessage();
    else
        message += e.getMessage();
    logger.error(message, e);
    ...
}
```

---

## How to Spot

Look for any `Throwable.getCause()` call whose result is used without a null check. The pattern is especially risky in `Future` or `ListenableFuture` failure callbacks, where the framework can deliver both wrapped (`ExecutionException`) and unwrapped exceptions through the same handler. A reviewer should verify that every `getCause()` result is either guarded with an `instanceof` check on the outer exception type or tested for null before dereferencing.

---

## Test

No test added.
