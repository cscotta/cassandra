# Bug 2046: Typo "Unkown" in CREATE TABLE Error Message

**Commit**: 6db8ac3898 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CreateColumnFamilyStatement.java
**Category**: Wrong constant / default value

---

## Root Cause

The error message thrown when a PRIMARY KEY references an undefined column used the misspelling `"Unkown definition %s referenced in PRIMARY KEY"`. While functionally harmless, misspelled error messages degrade debuggability and are confusing to operators. The fix corrects the string to `"Unknown definition %s referenced in PRIMARY KEY"`.

### Before / After

```java
// BEFORE
throw new InvalidRequestException(String.format("Unkown definition %s referenced in PRIMARY KEY", t));
```

```java
// AFTER
throw new InvalidRequestException(String.format("Unknown definition %s referenced in PRIMARY KEY", t));
```

---

## How to Spot

Spell-check user-visible exception messages and log strings during review. Static analysis tools (e.g., SpotBugs, custom lint rules) or a simple dictionary scan over string literals can catch misspellings before they ship.

---

## Test

No test added.
