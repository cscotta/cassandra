# Bug 0177: ProgressBarrier Timeout Logged with Inverted Duration

**Commit**: 0989a219ad · **Ticket**: CASSANDRA-19094
**Subsystem**: tcm/sequences/ProgressBarrier, simulator/asm
**Category**: Logic error in condition

---

## Root Cause

`ProgressBarrier.collectResponses` computed the elapsed wait time for its warning log as `Clock.Global.nanoTime() - deadline.deadlineNanos`. Because `deadline.deadlineNanos` is a future timestamp (start + timeout), this expression produces a large negative number (or an astronomically large unsigned value) rather than the actual elapsed milliseconds. The fix captures `start = Clock.Global.nanoTime()` before the barrier begins and uses `deadline.deadlineNanos - start` to compute the configured window. A secondary issue in the simulator ASM transformer also failed to intercept `Striped64.getProbe` / `advanceProbe`, causing non-deterministic random probe values to escape the simulation's control.

### Before / After

```java
// BEFORE
logger.warn("... within {}ms. Nodes that have not responded: {}",
            cl, waitFor,
            TimeUnit.NANOSECONDS.toMillis(Clock.Global.nanoTime() - deadline.deadlineNanos),
            remaining);
```

```java
// AFTER
long start = Clock.Global.nanoTime();
// ... barrier runs ...
logger.warn("... within {}ms. Nodes that have not responded: {}. {}",
            cl, waitFor,
            TimeUnit.NANOSECONDS.toMillis(deadline.deadlineNanos - start),
            remaining, deadline);
```

---

## How to Spot

Any log statement that formats a duration as `currentTime - futureDeadline` has the operands reversed; the duration should always be `deadline - startTime`. Reviewers should flag subtractions involving a pre-computed deadline timestamp on the left-hand side as a likely sign error.

---

## Test

The fix resolves `HarrySimulatorTest.harryTest`; no standalone unit test for the log message calculation was added.
