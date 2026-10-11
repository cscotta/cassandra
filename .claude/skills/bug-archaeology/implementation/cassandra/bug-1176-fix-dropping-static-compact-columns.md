# Bug 1176: Fix Dropping Static Compact Columns via Thrift

**Commit**: c0400b312b · **Ticket**: CASSANDRA-9867
**Subsystem**: src/java/org/apache/cassandra/schema/SchemaKeyspace.java
**Category**: Logic error in condition

---

## Root Cause

When Thrift issued a `DROP COLUMN` operation, `SchemaKeyspace` was supposed to skip non-REGULAR columns (because Thrift does not know about STATIC or other internal column kinds). The guard condition unconditionally skipped all non-REGULAR columns, meaning that STATIC columns on a static compact table could never be dropped via Thrift even when that was the explicit intent. The correct logic is to allow dropping a column if the request does not come from Thrift, OR if the column is REGULAR, OR if the table is a static compact table and the column is STATIC.

### Before / After

```java
// BEFORE
if (fromThrift && column.kind != ColumnDefinition.Kind.REGULAR) // TODO FIXME
    continue;

dropColumnFromSchemaMutation(oldTable, column, timestamp, mutation);
```

```java
// AFTER
if (!fromThrift ||
    column.kind == ColumnDefinition.Kind.REGULAR ||
    (newTable.isStaticCompactTable() && column.kind == ColumnDefinition.Kind.STATIC))
{
    dropColumnFromSchemaMutation(oldTable, column, timestamp, mutation);
}
```

---

## How to Spot

Any time a boolean guard is used to suppress an action "for safety" but a legitimate use case requires that same action, the condition is incomplete. Look for `TODO FIXME` comments adjacent to such guards — they are explicit markers that the logic has known holes. Review whether each suppressed case is truly invalid or just an incomplete enumeration of allowed cases.

---

## Test

`ThriftCompatibilityTest` is extended to verify that a static compact table column can be dropped through the Thrift path and the schema reflects the removal correctly.
