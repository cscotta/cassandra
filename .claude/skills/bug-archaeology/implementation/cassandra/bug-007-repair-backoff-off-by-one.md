# Bug 007: Fix off-by-one bug in exponential backoff for repair retry config

**Commit**: 386183fce1 · **Ticket**: CASSANDRA-21102
**Subsystem**: Retry / TimeoutStrategy
**Category**: Off-by-one

---

## Root Cause

The `multiplyByAttemptsExp` lambda in `TimeoutStrategy.LatencyModifier` computed the wait as `baseLatency * base^attempts`. Because `attempts` starts at 1 for the first retry, the first backoff was already multiplied by `base^1` rather than `base^0 = 1`, meaning the initial wait was immediately inflated by the base factor instead of starting at the configured base latency. The fix subtracts 1 from `attempts` (floored at 0) so that the first retry yields `base^0 = 1` and subsequent retries scale geometrically as intended.

### Before / After

```java
// BEFORE — first attempt (a=1) gives baseLatency * base^1, skipping the base value
default LatencyModifier multiplyByAttemptsExp(double base) {
    return (l, a) -> saturatedCast(l * pow(base, a));
}

// AFTER — first attempt (a=1) gives baseLatency * base^0 = baseLatency
default LatencyModifier multiplyByAttemptsExp(double base) {
    return (l, a) -> saturatedCast(l * pow(base, max(0, (max(a, 0) - 1))));
}
```

---

## How to Spot

Exponential backoff formulas of the form `constant^attempts` are off-by-one whenever the attempt counter is 1-based but the formula is written as if it were 0-based. Verify the boundary condition manually: substitute the minimum expected `attempts` value (typically 1) and confirm the result equals the configured base delay with no scaling applied. If the first call already multiplies by the base, the index needs to be decremented by one.

---

## Test

`TimeoutStrategyTest.testParseLatencyModifierExponential` parses the spec `"30ms * 2^attempts"` and asserts that attempt 1 returns exactly 30 ms, attempt 2 returns 60 ms, attempt 3 returns 120 ms, and that zero or negative attempt values also return the base latency.
