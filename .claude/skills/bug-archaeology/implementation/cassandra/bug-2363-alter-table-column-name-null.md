# Bug 2363: CQL3 ALTER TABLE constructor leaves columnName null, causing NPE

**Commit**: 36a9f9bd8f · **Ticket**: CASSANDRA-4163

## Detection Rule

When a constructor that takes a `columnName` parameter assigns `this.columnName = null` unconditionally (ignoring the parameter), any subsequent code path that dereferences `this.columnName` will throw a NPE.

## Code Smell

`AlterTableStatement(name, type, columnName, validator, propertyMap)` contains `this.columnName = null` — the parameter `columnName` is accepted but discarded, so every ALTER COLUMN or ADD COLUMN statement produces a NPE when the executor tries to look up the column.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: constructor-ignores-parameter
- severity: crash
- subsystem: cql-query

## Root Cause

A copy-paste or incorrect patch left the `AlterTableStatement` constructor assigning `this.columnName = null` instead of `this.columnName = columnName`. Every CQL3 `ALTER TABLE … ALTER COLUMN` or `ALTER TABLE … ADD COLUMN` statement would therefore arrive at `announceMigration()` with a null column name, causing a NPE when it tried to look up or operate on that column. The single-line fix assigns the passed-in `columnName` parameter.

## Example

```java
// BEFORE
public AlterTableStatement(CFName name, Type type, ColumnIdentifier columnName,
                            String validator, Map<String, String> propertyMap) {
    super(name);
    this.oType = type;
    this.columnName = null;     // bug: ignores the parameter
    this.validator = validator;
    ...
}
```

```java
// AFTER
    this.columnName = columnName;   // use the actual parameter
```
