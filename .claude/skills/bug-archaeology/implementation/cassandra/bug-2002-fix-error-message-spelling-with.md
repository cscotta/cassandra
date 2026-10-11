# Bug 2002: Typo "reation" in CQL SELECT Error Message

**Commit**: 155afa1b7f · **Ticket**: CASSANDRA-5647
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Logic error in condition

---

## Root Cause

The error message thrown when a column is restricted by more than one relation including an `IN` clause contained a typo: "reation" instead of "relation". While not a functional correctness bug, the misspelling was visible to end users and developers diagnosing query errors, making it harder to understand the error message.

### Before / After

```java
// BEFORE
throw new InvalidRequestException(String.format(
    "%s cannot be restricted by more than one reation if it includes a IN", name));
```

```java
// AFTER
throw new InvalidRequestException(String.format(
    "%s cannot be restricted by more than one relation if it includes a IN", name));
```

---

## How to Spot

Error message string literals are rarely tested and easy to mis-type. A spell-checker or search for common English words in string literals can catch these before they reach production.

---

## Test

No test added.
