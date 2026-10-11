# Bug 0537: Inaccurate Exception Message in nodetool snapshot with kt-list

**Commit**: a145a48520 · **Ticket**: CASSANDRA-15287
**Subsystem**: `src/java/org/apache/cassandra/tools/nodetool/Snapshot.java`
**Category**: Wrong constant / default value

---

## Root Cause

The error message thrown when a user illegally combines `--kt-list` (keyspace-table list) with a positional keyspace argument contained a misspelling ("columfamily" instead of "columnfamily") and described the wrong constraint—it said "you should not specify columnfamily" when in fact it was the positional keyspace argument that was disallowed. The misleading message made the user interface confusing because it referenced the wrong parameter, leading operators to look for the wrong flag when reading the error.

### Before / After

```java
// BEFORE
throw new IOException(
    "When specifying the Keyspace columfamily list for a snapshot, " +
    "you should not specify columnfamily");
```

```java
// AFTER
throw new IOException(
    "When specifying the Keyspace table list (using -kt,--kt-list,-kc,--kc.list), " +
    "you must not also specify keyspaces to snapshot");
```

---

## How to Spot

When reviewing CLI validation code, compare each error message against the actual condition being checked: the message should identify the offending flag or argument by its documented name. A message that names a different parameter than the one being validated is always wrong, and typos in parameter names in error strings are a common sign that the message was written without reference to the actual option definitions.

---

## Test

No test added.
