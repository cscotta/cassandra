# Bug 1063: LogFile.make Throws AssertionError When Assertions Are Disabled

**Commit**: a52597d813 · **Ticket**: CASSANDRA-10522
**Subsystem**: SSTable Lifecycle / LogFile
**Category**: Logic error in condition

---

## Root Cause
`LogFile.make()` called `matcher.matches()` as part of a combined `assert` expression: `assert matcher.matches() && matcher.groupCount() == 3`. When Java assertions are disabled (the default in production), the `assert` statement is a no-op — `matcher.matches()` is never called, so the matcher's internal state is never initialized. Any subsequent call to `matcher.group()` or `matcher.groupCount()` then throws an `IllegalStateException` ("No match found") or a related `AssertionError` because the match result is absent. The fix separates the side-effecting call `matcher.matches()` into a local variable before the `assert`, ensuring the match is always executed regardless of whether assertions are enabled.

### Before / After
```java
// BEFORE — matcher.matches() only runs when assertions are enabled
assert matcher.matches() && matcher.groupCount() == 3;

// AFTER — matcher.matches() always runs; result captured for the assertion
boolean matched = matcher.matches();
assert matched && matcher.groupCount() == 3;
```

---

## How to Spot
Any `assert` expression that contains a method call with side effects (state initialization, I/O, etc.) is broken when assertions are disabled, because the JVM simply skips the entire statement. In code review, flag `assert expr.sideEffectingMethod()` patterns and require the side-effecting call to be hoisted into a variable before the `assert`. This is especially dangerous for `Matcher.matches()` / `Matcher.find()` calls, which must precede any group access.

---

## Test
No test added.
