# Bug 1567: Fix ClassCastException Processing Expired Messages

**Commit**: 48ff36a457 · **Ticket**: CASSANDRA-7496
**Subsystem**: net / MessagingService
**Category**: Missing null / bounds check

---

## Root Cause

In the expired-message handler inside `MessagingService`, the code unconditionally cast `expiredCallbackInfo` to `WriteCallbackInfo` and extracted the `mutation` payload before checking whether hinting was even needed (`expiredCallbackInfo.shouldHint()`). If the expired callback was not a `WriteCallbackInfo` — e.g. a read callback — the cast threw a `ClassCastException`. The fix moves the cast and payload extraction inside the `shouldHint()` guard, so the downcast only happens when it is safe to do so.

### Before / After

```java
// BEFORE – unconditional cast before the shouldHint guard
Mutation mutation = (Mutation) ((WriteCallbackInfo) expiredCallbackInfo).sentMessage.payload;
try {
    if (expiredCallbackInfo.shouldHint()) {
        return StorageProxy.submitHint(mutation, expiredCallbackInfo.target, null);
    }
} finally {
    mutation.release();
}
return null;
```

```java
// AFTER – cast only performed inside the shouldHint branch
if (expiredCallbackInfo.shouldHint()) {
    Mutation mutation = (Mutation) ((WriteCallbackInfo) expiredCallbackInfo).sentMessage.payload;
    try {
        return StorageProxy.submitHint(mutation, expiredCallbackInfo.target, null);
    } finally {
        mutation.release();
    }
}
return null;
```

---

## How to Spot

Any downcast that precedes its own guard condition is suspicious. The rule is: narrow types only after confirming the object is of that type. During review, look for casts that appear before the `if` or early-return that justifies them.

---

## Test

No test added. The fix is a reordering of existing logic to move the downcast inside its guard.
