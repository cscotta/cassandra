# Bug 1648: Fix column name in invalid constant type error msg

**Commit**: 70fd478271 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3
**Category**: Wrong constant / default value

---

## Root Cause

In `Constants.java`, the `prepare()` method formats an error message when a literal value cannot be assigned to a column. The format string passed `receiver` (the `ColumnSpecification` object) as the column identifier, so `toString()` on the object was used, which included both the column name and its type information. The fix passes `receiver.name` (just the column name) and wraps it in quotes, producing a cleaner and accurate error that shows only the column name.

### Before / After

```java
// BEFORE
throw new InvalidRequestException(String.format(
    "Invalid %s constant (%s) for %s of type %s",
    type, text, receiver, receiver.type.asCQL3Type()));
```

```java
// AFTER
throw new InvalidRequestException(String.format(
    "Invalid %s constant (%s) for \"%s\" of type %s",
    type, text, receiver.name, receiver.type.asCQL3Type()));
```

---

## How to Spot

When building error messages that reference a named object (column, table, keyspace), check that the format argument is the `.name` field (or equivalent string accessor), not the object itself—unless the object's `toString()` is explicitly designed for user-facing output.

---

## Test

No test added.
