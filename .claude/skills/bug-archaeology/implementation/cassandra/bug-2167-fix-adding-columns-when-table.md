# Bug 2167: ALTER TABLE ADD on a table with collection columns uses wrong component index

**Commit**: 3a097ccff5 · **Ticket**: CASSANDRA-4982
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/AlterTableStatement.java
**Category**: Off-by-one

---

## Root Cause

When `ALTER TABLE ... ADD` appends a non-collection column to a table that already has collection columns, the composite comparator for CQL3 tables contains an extra `ColumnToCollectionType` component at the end. The code maintained a `componentIndex` to track where in the composite type to insert the new `ColumnDefinition`, but it only decremented `componentIndex` when the new column itself was a collection type — not when the table already had collections. As a result, `componentIndex` was one too high, causing the new column's definition to be written at the wrong position in the composite type and making the column inaccessible.

### Before / After

```java
// BEFORE - componentIndex not adjusted when existing table has collections
if (type instanceof CollectionType) {
    // ... update ColumnToCollectionType ...
    cfm.comparator = CompositeType.getInstance(ctypes);
}
// falls through to addColumnDefinition with wrong componentIndex
```

```java
// AFTER - componentIndex decremented for tables that already have collections
if (type instanceof CollectionType) {
    // ... update ColumnToCollectionType ...
    cfm.comparator = CompositeType.getInstance(ctypes);
}
else if (cfDef.hasCollections) {
    componentIndex--;
}
```

---

## How to Spot

When inserting into a composite type whose component count varies depending on schema features (like collections adding a trailing `ColumnToCollectionType`), verify that any index variable is adjusted for all cases where extra components may be present, not just the case where the new column itself introduces the extra component.

---

## Test

No new test added in this commit.
