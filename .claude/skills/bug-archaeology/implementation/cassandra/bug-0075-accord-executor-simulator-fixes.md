# Bug 0075: Multiple AccordExecutor Simulator Failures

**Commit**: b50536f1a4 · **Ticket**: CASSANDRA-20240
**Subsystem**: service/accord/AccordExecutor.java, AccordExecutorInfiniteLoops.java, utils/ExecutorUtils.java
**Category**: Logic error in condition

---

## Root Cause

This commit fixes several independent bugs that collectively caused simulator test failures:

1. **Wrong `TimeUnit` passed to `awaitTermination`**: `AccordExecutorInfiniteLoops.awaitTermination` computed a `wait` deadline in nanoseconds but passed the original caller-supplied `unit` to `loop.awaitTermination(wait, unit)`. The loop expected the wait value in `TimeUnit.NANOSECONDS`, so the deadline was interpreted in whatever unit the caller used (e.g. milliseconds), causing premature timeouts or hangs.

2. **Missing `FAILING` case in `cancelExclusive` switch**: The switch over task state in `cancelExclusive` had no `case FAILING` entry. A task in `FAILING` state was silently falling through to the `default` case which did nothing (and was not flagged as an error). The fix adds an explicit `case FAILING` alongside `RUNNING` and `PERSISTING`, and changes the `default` to throw `UnhandledEnum` to catch future unhandled states.

3. **Unconditional `pauseExclusive` on exception path**: In `AccordExecutorAbstractLockLoop`, when an exception was thrown inside the lock loop, `pauseExclusive()` was called unconditionally even when the lock was not held by the executor thread (`isHeldByExecutor == false`), causing an incorrect pause of a thread that never held the lock.

4. **Integer overflow in `NotInterceptedSyncCondition.await`**: The nanosecond wait time was converted to milliseconds with `(wait + 999999) / 1000000`. For values of `wait` near `Long.MAX_VALUE`, adding `999999` caused a signed integer overflow, flipping the result negative, and passing a negative timeout to `monitor.wait()` which throws `IllegalArgumentException`. The fix clamps the result to `Long.MAX_VALUE` when overflow is detected.

### Before / After

```java
// BEFORE – wrong TimeUnit passed
if (!loop.awaitTermination(wait, unit))  // wait is in nanoseconds, unit is caller's unit
```

```java
// AFTER – always nanoseconds
if (!loop.awaitTermination(wait, TimeUnit.NANOSECONDS))
```

```java
// BEFORE – overflow in wait conversion
monitor.wait((wait + 999999) / 1000000);
```

```java
// AFTER – clamped to avoid overflow
wait = (wait + 999999) / 1000000;
if (wait < 0) wait = Long.MAX_VALUE;
monitor.wait(wait);
```

---

## How to Spot

When a time value is converted from one unit (nanoseconds) to another (milliseconds) via arithmetic, both overflow and unit-mismatch risks exist. During review, verify that: (a) the unit being passed to every timed-wait API matches the unit of the value, and (b) the conversion arithmetic cannot overflow for extreme input values (e.g., near `Long.MAX_VALUE`). Additionally, any `switch` over an enum should include a `default: throw new UnhandledEnum(...)` to make new unhandled states visible immediately.

---

## Test

Simulator tests (`PairOfSequencesAccordSimulation`) catch failures across concurrent cancel/fail/run task state transitions; the missing `FAILING` case is now covered by the exhaustive switch.
