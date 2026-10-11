# Bug 2393: CFMetaData uses raw column comparator instead of column-definition comparator for composite types

**Commit**: 23dd8c9682 · **Ticket**: CASSANDRA-3792

## Detection Rule

When schema serialization passes `comparator` directly to column-definition helpers, it breaks for composite and super-column types whose column definitions are keyed by only the last component of the composite comparator.

## Code Smell

Multiple calls in `CFMetaData` pass `comparator` (the full, possibly composite, column comparator) to `ColumnDefinition.toSchema` / `deleteFromSchema` instead of the purpose-built `getColumnDefinitionComparator()` which extracts the last-component type for composite and sub-column type for super-column families.

## Tags

- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-comparator-passed-to-serializer
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause

`CFMetaData` methods that serialize/deserialize column definitions (`toSchema`, `deleteFromSchema`, `apply`) were passing the raw `comparator` field directly. For composite types, column definitions are stored using only the last component of the composite comparator as the key; passing the full composite type caused incorrect serialization or lookup. The fix introduces `getColumnDefinitionComparator()` which correctly returns the last element of a `CompositeType`'s type list (or `subcolumnComparator` for super families), and all schema mutation methods now call this helper.

## Example

```java
// BEFORE
cd.toSchema(rm, cfName, comparator, modificationTimestamp);  // wrong for CompositeType
cd.deleteFromSchema(rm, cfName, comparator, modificationTimestamp);
oldDef.apply(def, comparator);
```

```java
// AFTER
cd.toSchema(rm, cfName, getColumnDefinitionComparator(), modificationTimestamp);
cd.deleteFromSchema(rm, cfName, getColumnDefinitionComparator(), modificationTimestamp);
oldDef.apply(def, getColumnDefinitionComparator());

// new helper:
public AbstractType<?> getColumnDefinitionComparator() {
    AbstractType<?> cfComparator = cfType == ColumnFamilyType.Super ? subcolumnComparator : comparator;
    if (cfComparator instanceof CompositeType)
        return ((CompositeType)cfComparator).types.get(types.size() - 1);
    return cfComparator;
}
```
