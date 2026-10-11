# Bug 1727: CAS Conditions Fail for COMPACT STORAGE Tables

**Commit**: f3be42fc1a · **Ticket**: CASSANDRA-6813
**Subsystem**: src/java/org/apache/cassandra/cql3/ColumnCondition.java, CQL3CasConditions.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Two related problems caused CAS (lightweight transaction) `IF` conditions to malfunction for COMPACT STORAGE tables.

First, in `ColumnCondition.appliesTo`, the column name was always constructed by appending `column.name.key` to the row prefix. For COMPACT STORAGE tables, the single value column has kind `VALUE_ALIAS` and its storage name is the prefix itself (built without appending), so the lookup found nothing and the condition always evaluated as if the column were null.

Second, in `CQL3CasConditions.readFilter`, the `toGroup` parameter passed to `SliceQueryFilter` was always set to the clustering key column count, which is the correct grouping for CQL3 tables. For compact tables, grouping must be `-1` (no grouping), otherwise the filter returned the wrong set of columns.

### Before / After

```java
// BEFORE (ColumnCondition)
Column c = current.getColumn(
    copyOrUpdatePrefix(current.metadata(), rowPrefix).add(column.name.key).build());
```

```java
// AFTER (ColumnCondition)
ColumnNameBuilder prefix = copyOrUpdatePrefix(current.metadata(), rowPrefix);
ByteBuffer columnName = column.kind == CFDefinition.Name.Kind.VALUE_ALIAS
                      ? prefix.build()
                      : prefix.add(column.name.key).build();
Column c = current.getColumn(columnName);
```

```java
// BEFORE (CQL3CasConditions)
return new SliceQueryFilter(slices, false, slices.length,
    cfm.clusteringKeyColumns().size());
```

```java
// AFTER (CQL3CasConditions)
int toGroup = cfm.getCfDef().isCompact ? -1 : cfm.clusteringKeyColumns().size();
return new SliceQueryFilter(slices, false, slices.length, toGroup);
```

---

## How to Spot

Any CQL path that constructs column names or query filters should branch on `isCompact` (or equivalent) when COMPACT STORAGE tables have a structurally different column layout. Look for code that applies a single name-building strategy without checking whether the column is a `VALUE_ALIAS`.

---

## Test

No test added.
