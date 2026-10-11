# Bug 2473: NPE in get_count for counter columns — assumes column union type is always a regular column

**Commit**: 511d16321e · **Ticket**: CASSANDRA-3601

## Detection Rule

When a Thrift `ColumnOrSuperColumn` union type is accessed without checking which field is actually set, a NullPointerException occurs for any non-regular column variant (counter column, counter super-column).

## Code Smell

`lastColumn.column.name` accessed directly without checking `isSetColumn()`, `isSetCounter_column()`, or `isSetCounter_super_column()` — only `isSetSuper_column()` was checked, leaving three other union branches unhandled.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: unchecked-union-field-access
- severity: crash
- subsystem: cql-query

## Root Cause

In `CassandraServer.get_count()`, the pagination loop accessed `lastColumn.column.name` as a fallback after checking only for super-columns. Counter columns and counter super-columns are separate union variants in the Thrift `ColumnOrSuperColumn` struct; accessing `.column` on a counter column returns null, causing an NPE.

## Example

```java
// BEFORE
ByteBuffer lastName = lastColumn.isSetSuper_column() 
    ? lastColumn.super_column.name 
    : lastColumn.column.name;  // NPE if counter column
```

```java
// AFTER
ByteBuffer lastName =
    lastColumn.isSetSuper_column() ? lastColumn.super_column.name :
        (lastColumn.isSetColumn() ? lastColumn.column.name :
            (lastColumn.isSetCounter_column() ? lastColumn.counter_column.name 
                : lastColumn.counter_super_column.name));
```
