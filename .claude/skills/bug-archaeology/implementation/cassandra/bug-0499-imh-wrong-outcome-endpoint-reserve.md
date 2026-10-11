# Bug 0499: IMH acquireCapacity Returns Wrong Outcome on Endpoint Reserve Exhaustion

**Commit**: aef3bec087 · **Ticket**: CASSANDRA-15607
**Subsystem**: `src/java/org/apache/cassandra/net/InboundMessageHandler.java`
**Category**: Wrong constant / default value

---

## Root Cause

`InboundMessageHandler#acquireCapacity()` checks the per-endpoint queue reserve first and then the global reserve. When the per-endpoint reserve was exhausted (the failure condition that triggered the error path), the code returned `INSUFFICIENT_GLOBAL` instead of `INSUFFICIENT_ENDPOINT`. Callers that distinguish between the two outcomes — to decide whether to throttle a single endpoint or the whole node — would therefore take the wrong action, potentially discarding or blocking messages from all endpoints rather than only the one that ran out of reserve.

### Before / After

```java
// BEFORE — wrong outcome constant; reports global limit when endpoint limit was hit
globalReserve.release(allocatedExcess);
globalWaitQueue.signal();
return ResourceLimits.Outcome.INSUFFICIENT_GLOBAL;
```

```java
// AFTER — correctly reports the endpoint limit as the binding constraint
globalReserve.release(allocatedExcess);
globalWaitQueue.signal();
return ResourceLimits.Outcome.INSUFFICIENT_ENDPOINT;
```

---

## How to Spot

In methods that perform a multi-level resource check (endpoint reserve, then global reserve), each early-return path must return the outcome constant that matches the level where the check failed. When reviewing, trace every return statement back to the condition that guards it, and verify the constant name matches the guard's semantics. Copy-paste errors are easy to introduce when similar blocks handle different quota levels.

---

## Test

No test added.
