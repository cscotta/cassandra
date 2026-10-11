# Bug 0462: Hinted Handoff Per-Host Throttle Rounds Down to Zero in Large Clusters

**Commit**: 979a71b13a · **Ticket**: CASSANDRA-15859
**Subsystem**: `src/java/org/apache/cassandra/hints/HintsDispatchExecutor.java`
**Category**: Wrong constant / default value

---

## Root Cause

The per-host hints throttle was computed using integer arithmetic: `getHintedHandoffThrottleInKB() / nodesCount`. In a large cluster where `nodesCount` exceeded the configured throttle value (expressed in kilobytes), the integer division rounded the result down to zero. The code then treated a zero throttle as "unlimited" (`Double.MAX_VALUE`), the exact opposite of the intended behavior — a very large cluster would effectively have no throttle instead of a near-zero one. The fix changes the division to floating-point arithmetic by multiplying by 1024.0 first, so sub-kilobyte-per-node rates are preserved as fractional byte values rather than being truncated to zero.

### Before / After

```java
// BEFORE — integer division truncates to 0 when nodesCount > throttleInKB
int throttleInKB = DatabaseDescriptor.getHintedHandoffThrottleInKB() / nodesCount;
this.rateLimiter = RateLimiter.create(
    throttleInKB == 0 ? Double.MAX_VALUE : throttleInKB * 1024);
```

```java
// AFTER — floating-point division preserves fractional bytes-per-second rate
double throttleInBytes = DatabaseDescriptor.getHintedHandoffThrottleInKB() * 1024.0 / nodesCount;
this.rateLimiter = RateLimiter.create(
    throttleInBytes == 0 ? Double.MAX_VALUE : throttleInBytes);
```

---

## How to Spot

Throttle or rate calculations that divide an integer configuration value by a runtime count are susceptible to truncation when the divisor grows larger than the dividend. Reviewers should verify that rate or quota computations use floating-point arithmetic and that the "zero means unlimited" sentinel is checked against the floating-point result rather than the pre-division integer, to avoid the paradox where a stricter cluster size accidentally disables the throttle entirely.

---

## Test

No test added.
