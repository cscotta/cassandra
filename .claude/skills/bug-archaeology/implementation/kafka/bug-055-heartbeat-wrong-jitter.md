**Commit**: 0facc3ccec · **Ticket**: KAFKA-19813
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractHeartbeatRequestManager.java
**Category**: Wrong constant / default value

**Root cause**
`AbstractHeartbeatRequestManager` and `StreamsGroupHeartbeatRequestManager` both constructed their `HeartbeatRequestState` using `maxPollIntervalMs` (default 300,000 ms) as the jitter argument. The jitter parameter for `ExponentialBackoff` must be in the range `[0, 1]` — a dimensionless fraction. Passing 300,000 is semantically nonsensical and would have caused `ExponentialBackoff` to produce wildly inflated backoff values (though `RequestState.remainingBackoffMs` clamped to zero, masking the symptom). The fix uses the constant `RETRY_BACKOFF_JITTER` (a proper fractional value) and adds validation in `ExponentialBackoff` that rejects out-of-range jitter.

**Before/After**
```java
// Before: maxPollIntervalMs (e.g. 300000) used as jitter fraction
this.heartbeatRequestState = new HeartbeatRequestState(logContext, time, 0,
        retryBackoffMs, retryBackoffMaxMs, maxPollIntervalMs);

// After: proper fractional jitter constant
this.heartbeatRequestState = new HeartbeatRequestState(logContext, time, 0,
        retryBackoffMs, retryBackoffMaxMs, RETRY_BACKOFF_JITTER);
```

**How to spot**
Review every call site that passes a `jitter` argument to `ExponentialBackoff` or classes that wrap it. A value greater than 1.0 or less than 0 is always wrong. The new validation in `ExponentialBackoff` constructor now catches this at construction time rather than silently misbehaving.

**Test**
`testExponentialBackoffWithInvalidJitter` asserts that `new ExponentialBackoff(100, 2, 400, -1)` and `new ExponentialBackoff(100, 2, 400, 3000)` both throw `IllegalArgumentException` with an appropriate message.
