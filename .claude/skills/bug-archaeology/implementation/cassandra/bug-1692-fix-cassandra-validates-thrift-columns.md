# Bug 1692: Fix Cassandra 2.0.x Validates Thrift Columns Incorrectly

**Commit**: b42feea643 · **Ticket**: CASSANDRA-6892
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java, src/java/org/apache/cassandra/thrift/ThriftValidation.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CFMetaData.getColumnDefinitionFromColumnName()` returned any `ColumnDefinition` whose stored name matched the raw cell name, including `PARTITION_KEY` and `CLUSTERING_COLUMN` definitions. When a Thrift client wrote a cell whose name happened to match a CQL partition key alias (e.g., the default "key" alias) or a clustering column alias, `getValueValidator()` resolved to that alias's type instead of the table's `defaultValidator`. Validation then failed for perfectly valid Thrift data because the cell value did not conform to the alias type (e.g., UUID). The fix restricts `getColumnDefinitionFromColumnName` to return only definitions that are `REGULAR` or `STATIC` (i.e., definitions whose name actually appears as a cell name on disk), and renames the value-validator helper to `getValueValidatorFromColumnName` to make the contract explicit.

### Before / After

```java
// BEFORE – returns PARTITION_KEY def if name matches, wrong type used for validation
return column_metadata.get(columnName);
```

```java
// AFTER – only return defs whose name is part of an actual cell name
ColumnDefinition def = column_metadata.get(columnName);
return def != null && def.isPartOfCellName() ? def : null;
```

---

## How to Spot

When looking up a `ColumnDefinition` by cell name, distinguish between definitions that describe actual on-disk cell names (REGULAR/STATIC) and those that are purely CQL metadata aliases (PARTITION_KEY/CLUSTERING_COLUMN). Returning the wrong category silently applies the wrong type validator.

---

## Test

`ThriftValidationTest.testColumnNameEqualToDefaultKeyAlias()` and `testColumnNameEqualToDefaultColumnAlias()` were added; `ScrubTest.testColumnNameEqualToDefaultKeyAlias()` and `testValidationCompactStorage()` were also added to cover scrub-path validation.
