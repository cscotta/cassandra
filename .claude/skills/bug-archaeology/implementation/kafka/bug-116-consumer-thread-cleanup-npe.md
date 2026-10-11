**Commit**: 692c7f14d6 · **Ticket**: KAFKA-19206
**Subsystem**: clients / consumer / internals / ConsumerNetworkThread
**Category**: Missing null / bounds check

**Root cause**
`ConsumerNetworkThread.cleanup()` called `runAtClose(requestManagers.entries(), ...)` and `sendUnsentRequests(timer)` unconditionally. Both `requestManagers` and `networkClientDelegate` are initialized lazily in `initializeResources()` via `Supplier.get()`. If either supplier threw an exception during initialization the fields remained null, and the subsequent `cleanup()` call dereferenced them, producing a `NullPointerException`.

**Before/After**
```java
// Before — unconditional use of possibly-null fields
runAtClose(requestManagers.entries(), networkClientDelegate, time.milliseconds());
// ...
sendUnsentRequests(timer);

// After — null checks guard both calls
if (requestManagers != null && networkClientDelegate != null)
    runAtClose(requestManagers.entries(), networkClientDelegate, time.milliseconds());
if (networkClientDelegate != null)
    sendUnsentRequests(timer);
```

**How to spot**
Any `close()` / `cleanup()` method should be idempotent and safe to call even when the object was only partially constructed. Fields that are set lazily or via fallible suppliers must be null-checked before use in cleanup paths.

**Test**
`ConsumerNetworkThreadTest.testNetworkClientDelegateInitializeResourcesError` / `testRequestManagersInitializeResourcesError` — inject a supplier that throws, call `initializeResources()`, then call `cleanup()` and assert no exception is thrown.
