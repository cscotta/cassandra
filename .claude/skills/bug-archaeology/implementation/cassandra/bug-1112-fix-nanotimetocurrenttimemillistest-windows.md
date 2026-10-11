# Bug 1112: NanoTimeToCurrentTimeMillis Test Fails on Windows Due to Tight Timing Tolerances

**Commit**: 06a053ddc9 · **Ticket**: CASSANDRA-10139
**Subsystem**: src/java/org/apache/cassandra/utils/NanoTimeToCurrentTimeMillis.java
**Category**: Wrong constant / default value

---

## Root Cause

The `NanoTimeToCurrentTimeMillisTest` used hard-coded timing tolerance values calibrated for Linux, where `System.nanoTime()` and `System.currentTimeMillis()` skew by at most ~2 ms. On Windows, the system clock has much lower resolution and the two clocks can diverge by up to ~25 ms, causing the assertions to fail. Additionally, the test contained a variable name bug (`now` used instead of `nowNanos`) that meant the `Math.max` guard was comparing a millisecond value against a nanosecond value, making the monotonicity guarantee ineffective.

### Before / After

```java
// BEFORE
nowNanos = Math.max(now, System.nanoTime());  // 'now' is millis, not nanos — wrong variable
assertTrue(..., convertedNow >= (lastConverted - 1));  // tolerance of 1ms, too tight for Windows
assertTrue(..., (now - 2) <= convertedNow);  // tolerance of 2ms, too tight for Windows
```

```java
// AFTER
nowNanos = Math.max(nowNanos, System.nanoTime());  // correct variable
int maxDiff = FBUtilities.isWindows() ? 15 : 1;
assertTrue(..., convertedNow >= (lastConverted - maxDiff));
maxDiff = FBUtilities.isWindows() ? 25 : 2;
assertTrue(..., (maxDiff - 2) <= convertedNow);
```

---

## How to Spot

When a test asserts timing precision with a literal constant tolerance (e.g. `- 1` or `- 2` milliseconds), check whether the assumption holds across all supported platforms. Windows system clocks are notoriously coarse compared to Linux; any timing test that passes only on Linux should use platform-conditional tolerances or `FBUtilities.isWindows()` guards. Also watch for cases where a variable used inside a monotonic guard (`Math.max`) has a different unit than the one being computed — mixing millis with nanos silently breaks the guard.

---

## Test

The existing `NanoTimeToCurrentTimeMillisTest` is the failing test; the fix widens the assertion tolerances per platform and corrects the wrong variable reference so the test passes on Windows.
