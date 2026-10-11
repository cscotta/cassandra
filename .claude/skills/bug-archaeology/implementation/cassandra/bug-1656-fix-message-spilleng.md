# Bug 1656: Misspelled Error Message in CREATE TRIGGER Authorization Check

**Commit**: daf54c5c73 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Logic error in condition

---

## Root Cause

The authorization error message returned when a non-superuser attempts a `CREATE TRIGGER` query contained the typo "perfrom" instead of "perform". While not a runtime fault, the misspelled string is emitted verbatim to clients as an `UnauthorizedException` message, producing misleading diagnostic output. The fix corrects the string literal in `CreateTriggerStatement.checkAccess()`.

### Before / After

```java
// BEFORE
state.ensureIsSuper("Only superusers are allowed to perfrom CREATE TRIGGER queries");
```

```java
// AFTER
state.ensureIsSuper("Only superusers are allowed to perform CREATE TRIGGER queries");
```

---

## How to Spot

Search error/exception message string literals for common homoglyphs and transposition typos. A spellchecker integrated into the build (e.g., a checkstyle rule or IDE inspection) would catch this class of defect before commit.

---

## Test

No test added.
