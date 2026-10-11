# Bug 0356: Full Stack Trace Logged and Exception Swallowed on Index Summary Redistribution Cancellation

**Commit**: ddcf3e6aba · **Ticket**: CASSANDRA-16865
**Subsystem**: src/java/org/apache/cassandra/io/sstable (IndexSummaryManager)
**Category**: Logic error in condition

---

## Root Cause

The exception handler in `IndexSummaryManager.redistributeSummaries()` checked `if (!(e instanceof CompactionInterruptedException))` and only logged the full error when the condition was true.  However, the `throw e` statement at the end of the `if` block meant cancellation (`CompactionInterruptedException`) was silently swallowed — no log message at all — while the redistribution failure went unnoticed by callers.  All other exceptions were logged at `ERROR` with a stack trace and then re-thrown, which is correct, but the inverted guard meant the wrong exceptions were being suppressed.

### Before / After

```java
// BEFORE
if (!(e instanceof CompactionInterruptedException))
    logger.error("Got exception during index summary redistribution", e);
throw e;
```

```java
// AFTER
if (e instanceof CompactionInterruptedException)
{
    logger.info("Index summary interrupted: {}", e.getMessage());
}
else
{
    logger.error("Got exception during index summary redistribution", e);
    throw e;
}
```

---

## How to Spot

When writing a guard of the form `if (!(x instanceof SomeException))`, verify what happens in both branches: the "is not" branch runs the special case for the unwanted exception type, and the normal path falls through to re-throw.  It is often clearer (and less error-prone) to invert the condition and handle the specific known exception first, letting the else branch handle the unexpected case.

---

## Test

No test added.
