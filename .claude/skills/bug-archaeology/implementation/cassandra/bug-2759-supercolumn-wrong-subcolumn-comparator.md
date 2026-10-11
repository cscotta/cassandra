# Bug 2759: Supercolumn Deserialization Uses Column Comparator Instead of Subcolumn Comparator

**Commit**: 53286d56f0 · **Ticket**: CASSANDRA-2104

## Detection Rule
When constructing a `SuperColumn` serializer from an `SSTableReader`, use `metadata.subcolumnComparator` rather than the top-level column comparator, since super-column sub-columns may have a different comparator than the super-column names.

## Code Smell
`SuperColumn.serializer(getColumnComparator())` passes the top-level column comparator to the super-column serializer; when the subcolumn comparator differs, bytes are decoded with the wrong type.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-comparator-for-subcolumns
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`SSTableReader.getColumnSerializer()` called `getColumnComparator()` which returns the comparator for super-column names. Super-column sub-columns are ordered by `metadata.subcolumnComparator`, which can be a completely different type. Passing the wrong comparator caused incorrect sort order and deserialization failures when the two comparators differed. The fix uses `metadata.subcolumnComparator` explicitly.

## Example
```java
// BEFORE
return metadata.cfType == ColumnFamilyType.Standard
    ? Column.serializer()
    : SuperColumn.serializer(getColumnComparator());
```
```java
// AFTER
return metadata.cfType == ColumnFamilyType.Standard
    ? Column.serializer()
    : SuperColumn.serializer(metadata.subcolumnComparator);
```
