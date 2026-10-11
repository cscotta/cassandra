# Bug 0725: NPE in back-pressure code when connection pool not yet established

**Commit**: c86de2a981 · **Ticket**: CASSANDRA-13324 (follow-up fix)
**Subsystem**: Messaging / back-pressure
**Category**: Missing null / bounds check

---

## Root Cause
The back-pressure feature introduced in CASSANDRA-13324 added calls to `getConnectionPool(host)` in several `MessagingService` methods (`updateBackPressureOnSend`, `updateBackPressureOnReceive`, `applyBackPressure`, and the callback expiry path). `getConnectionPool()` can return `null` when a connection pool for a host has not been created yet — for example during startup, after a node is removed, or when a message is sent to a host before its pool is initialized. The original commit did not guard any of these call sites, so any of the four paths could immediately dereference the null return value and throw an NPE. Additionally, the expiry path called `getConnectionPool(expiredCallbackInfo.target).incrementTimeout()` unconditionally instead of reusing the already-null-checked local variable `cp`. The fix adds `if (cp != null)` guards at every site and converts the stream-based `applyBackPressure` loop to an explicit for-loop to allow the null check.

### Before / After
```java
// BEFORE — updateBackPressureOnSend, no null check
BackPressureState backPressureState = getConnectionPool(host).getBackPressureState();
backPressureState.onMessageSent(message);

// AFTER
OutboundTcpConnectionPool cp = getConnectionPool(host);
if (cp != null)
{
    BackPressureState backPressureState = cp.getBackPressureState();
    backPressureState.onMessageSent(message);
}

// BEFORE — callback expiry, double lookup
getConnectionPool(expiredCallbackInfo.target).incrementTimeout();

// AFTER — removed; the already-null-checked cp.incrementTimeout() above is sufficient
```

---

## How to Spot
Any call to a factory or registry method that can return null should be assigned to a local variable and null-checked before use. When the same lookup result is needed more than once, the second use must reuse the local variable rather than calling the lookup again — both to avoid a double null hazard and to avoid redundant work. Stream pipelines that chain `.map(h -> getConnectionPool(h).getX())` hide null dereferences inside lambdas where they are hard to see.

---

## Test
No test added.
