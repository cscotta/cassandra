# Bug 2009: Swapped Arguments in AlreadyExistsException Error Message

**Commit**: 27e8f87ff1 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/exceptions/AlreadyExistsException.java
**Category**: Wrong constant / default value

---

## Root Cause

`AlreadyExistsException` constructs a human-readable error message using `String.format()` with the format string `"Cannot add already existing column family \"%s\" to keyspace \"%s\""`. The first placeholder is for the column family name and the second for the keyspace name, but the arguments were passed in reverse order — `ksName` was passed where `cfName` was expected. As a result, users saw the keyspace name in place of the column family name and vice versa in the exception message.

### Before / After

```java
// BEFORE
this(ksName, cfName, String.format(
    "Cannot add already existing column family \"%s\" to keyspace \"%s\"",
    ksName, cfName));  // ksName and cfName swapped
```

```java
// AFTER
this(ksName, cfName, String.format(
    "Cannot add already existing column family \"%s\" to keyspace \"%s\"",
    cfName, ksName));  // correct order
```

---

## How to Spot

When calling `String.format()` with multiple `%s` placeholders, verify that the argument list order matches the placeholder order in the format string. This class of bug is especially common when the parameter names resemble each other (e.g., `ksName`/`cfName`) and the format string lists them in non-alphabetical order.

---

## Test

No test added.
