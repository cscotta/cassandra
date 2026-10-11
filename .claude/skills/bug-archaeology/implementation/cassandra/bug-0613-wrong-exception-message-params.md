# Bug 0613: AlterTableStatement Exception Reports Wrong Table Name

**Commit**: 7df36056b1 · **Ticket**: n/a
**Subsystem**: `src/java/org/apache/cassandra/cql3/statements/AlterTableStatement.java`
**Category**: Wrong constant / default value

---

## Root Cause

The format string for the "cannot drop column on base table with materialized views" exception included two `%s` placeholders — one for the column name and one for the table name — but the arguments list only provided `columnName.toString()` and `keyspace()`. The keyspace name (e.g., `mykeyspace`) was substituted where the table name should appear, making the error message misleading. The fix replaces `keyspace()` with `columnFamily()` to produce the correct table name.

### Before / After

```java
// BEFORE
throw new InvalidRequestException(String.format(
    "Cannot drop column %s on base table with materialized views.",
    columnName.toString(),
    keyspace()));
```

```java
// AFTER
throw new InvalidRequestException(String.format(
    "Cannot drop column %s on base table %s with materialized views.",
    columnName.toString(),
    columnFamily()));
```

---

## How to Spot

Any `String.format` call where the number of `%s` placeholders does not match the number of arguments, or where argument order looks suspicious, is worth reviewing. Static analysis tools and IDE inspections typically flag mismatches; a reviewer should also verify that `keyspace()` vs. `columnFamily()` is used intentionally when building entity-identifying messages.

---

## Test

No test added.
