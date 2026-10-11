# Bug 0645: Integer Overflow in CDC Total Space Computation on Large Filesystems

**Commit**: 0bc45aa467 · **Ticket**: CASSANDRA-13808
**Subsystem**: config/DatabaseDescriptor
**Category**: Wrong constant / default value

---

## Root Cause

The default `cdc_total_space_in_mb` is computed as one-sixteenth of the raw filesystem capacity. The intermediate arithmetic divides `getTotalSpace()` (a `long`) by 1 MiB and then by 8, and passes the result to `Ints.checkedCast()`. On filesystems at exabyte scale the total space expressed in MiB can still exceed `Integer.MAX_VALUE`, causing `Ints.checkedCast()` to throw an `ArithmeticException` at startup. The fix replaces `checkedCast` with `saturatedCast`, which silently clamps the result to `Integer.MAX_VALUE` instead of throwing, yielding the largest representable `int` value (approximately 2 PiB worth of MB) rather than crashing.

### Before / After

```java
// BEFORE
minSize = Ints.checkedCast(
    (guessFileStore(conf.cdc_raw_directory).getTotalSpace() / 1048576) / 8);
```

```java
// AFTER
minSize = Ints.saturatedCast(
    (guessFileStore(conf.cdc_raw_directory).getTotalSpace() / 1048576) / 8);
```

---

## How to Spot

Any call to `Ints.checkedCast()` (or an explicit `(int)` cast) on a value derived from filesystem or memory size queries (`getTotalSpace()`, `getFreeSpace()`, `Runtime.maxMemory()`, etc.) should be audited for overflow at non-standard scales. The safe pattern is either `saturatedCast` for a best-effort cap, or reformulating the arithmetic so the division happens before the cast to keep intermediate values within `int` range. Static analysis for narrowing casts on `long` expressions sourced from OS calls will surface these sites.

---

## Test

No test added.
