# Bug 0560: NPE in RepairRunnable When Failure Message Is Null

**Commit**: 98d81e409a · **Ticket**: CASSANDRA-15025
**Subsystem**: src/java/org/apache/cassandra/repair
**Category**: Missing null / bounds check

---

## Root Cause

`RepairRunnable.recordFailure` passed the raw exception message directly into `ImmutableList.of(failureMessage, completionMessage)`. When an exception carries a null message (which is legal in Java), this produces a list containing `null`, and downstream code that inspects the first element as the failure reason receives `null` and throws another NPE. A secondary issue was that error log calls omitted the `parentSession` identifier and the exception object, making failures undiagnosable.

### Before / After

```java
// BEFORE
recordFailure(message, completionMessage);
...
ActiveRepairService.instance.recordRepairStatus(cmd, FAILED,
    ImmutableList.of(failureMessage, completionMessage));
```

```java
// AFTER
String errorMessage = String.format("Repair command #%d failed with error %s", cmd, message);
recordFailure(errorMessage, completionMessage);
...
String failure = failureMessage == null ? "unknown failure" : failureMessage;
String completion = completionMessage == null ? "unknown completion" : completionMessage;
ActiveRepairService.instance.recordRepairStatus(cmd, FAILED,
    ImmutableList.of(failure, completion));
```

---

## How to Spot

Whenever a method accepts a `String` argument sourced from `Throwable.getMessage()` and passes it onward without a null check, search for all downstream consumers that dereference the string. `Throwable.getMessage()` is documented to return `null` if no message was provided, so any code that assumes a non-null message from an exception is suspect. The pattern of passing the raw exception message directly into a collection or status record without guarding against null is a recurring category of NPE.

---

## Test

No test added.
