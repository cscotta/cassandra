# Bug 1954: Duplicate word in trigger exception message

**Commit**: 1a2208ad12 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/triggers/TriggerExecutor.java
**Category**: Logic error in condition

---

## Root Cause

The exception message thrown when trigger instantiation fails read "Exception while creating trigger a trigger on CF", with "trigger" duplicated due to a copy-paste error. While purely cosmetic, the garbled message complicates log triage and automated parsing that matches on the exact error string.

### Before / After

```java
// BEFORE
throw new RuntimeException(String.format(
    "Exception while creating trigger a trigger on CF with ID: %s",
    columnFamily.id()), ex);
```

```java
// AFTER
throw new RuntimeException(String.format(
    "Exception while creating trigger on CF with ID: %s",
    columnFamily.id()), ex);
```

---

## How to Spot

Exception and log messages that include the noun being operated on (e.g., "creating trigger") should be reviewed for accidental word repetition, especially when composed from template strings.

---

## Test

No test added.
