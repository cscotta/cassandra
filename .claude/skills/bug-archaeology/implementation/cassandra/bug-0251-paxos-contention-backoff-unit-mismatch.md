# Bug 0251: Paxos ContentionStrategy Backoff Applied in Wrong Units; `Clock.waitUntil` Subtraction Inverted

**Commit**: 81c616826a · **Ticket**: CASSANDRA-18086 (also CASSANDRA-18087)
**Subsystem**: `service/paxos/ContentionStrategy.java`, `utils/Clock.java`
**Category**: Logic error in condition

---

## Root Cause

Two independent bugs caused Paxos contention backoff to malfunction. First, `computeWaitUntilForContention()` computed `nanoTime() + wait` where `wait` was in **microseconds**, not nanoseconds; the missing `MICROSECONDS.toNanos(wait)` conversion made the computed deadline 1000× too close in the future, so backoff waits were essentially zero. Second, `Clock.waitUntil()` computed the sleep duration as `nanoTime() - deadlineNanos` (current minus target) instead of the correct `deadlineNanos - nanoTime()`, producing a negative value that was always ≤ 0, so the sleep was always skipped entirely.

### Before / After

```java
// BEFORE — ContentionStrategy.java
long wait = waitRandomizer.wait(minWaitMicros, maxWaitMicros, attempts);
return nanoTime() + wait;  // wait is microseconds, added to nanosecond clock

// BEFORE — Clock.java
long waitNanos = Clock.Global.nanoTime() - deadlineNanos;  // subtraction inverted
```

```java
// AFTER — ContentionStrategy.java
long wait = waitRandomizer.wait(minWaitMicros, maxWaitMicros, attempts);
return nanoTime() + MICROSECONDS.toNanos(wait);  // unit conversion applied

// AFTER — Clock.java
long waitNanos = deadlineNanos - Clock.Global.nanoTime();  // correct direction
```

---

## How to Spot

Whenever a computed duration is added to or subtracted from a timestamp, verify both operands carry the same time unit. Mixed-unit arithmetic (microseconds added to a nanosecond clock) never triggers a compile error. Similarly, when computing "how long until a deadline", confirm the subtraction order is `deadline - now`, not `now - deadline`. Both classes of mistake produce silent near-zero or always-negative durations that are easy to miss without unit tests that verify actual sleep lengths.

---

## Test

`ContentionStrategyTest` adds cases that exercise `computeWaitUntilForContention` and `doWaitForContention` with known backoff parameters, verifying that the returned deadline is meaningfully in the future.
