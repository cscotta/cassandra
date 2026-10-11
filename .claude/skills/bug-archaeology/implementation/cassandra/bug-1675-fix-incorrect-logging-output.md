# Bug 1675: Fix Incorrect Logging Output in Differencer

**Commit**: 66a6990aa0 · **Ticket**: n/a
**Subsystem**: repair/Differencer
**Category**: Logic error in condition

---

## Root Cause

A prior refactor changed the log call in `Differencer` but inadvertently swapped the role of the format string and the message argument. The call `logger.info(format, "have {} range(s) out of sync", differences.size())` passed `format` (a pre-built session-context string) as the SLF4J format pattern and the human-readable message as a positional argument, so `differences.size()` was never substituted and the number of out-of-sync ranges was silently dropped from the output. The fix collapses both parts into a single `String.format()` call so the full message is emitted correctly.

### Before / After

```java
// BEFORE
logger.info(format, "have {} range(s) out of sync", differences.size());
```

```java
// AFTER
logger.info(String.format(format, "have " + differences.size() + " range(s) out of sync"));
```

---

## How to Spot

When an SLF4J `logger.info(fmt, args...)` call receives a runtime-constructed string as the *first* argument (rather than a literal pattern), and the literal message is passed as a subsequent argument, the substitution will not work as intended. Review all logger calls that pass a variable as the format pattern.

---

## Test

No test added.
