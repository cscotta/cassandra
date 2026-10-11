# Bug 2408: AtomicSortedColumns used for super column families, corrupting counter updates

**Commit**: 9f90a7997a · **Ticket**: CASSANDRA-3821

## Detection Rule

When a concurrent data structure is selected by a single code path without checking column family type, and that structure does not support the operations required by one of the types, add a type-dispatch to select the appropriate implementation.

## Code Smell

`AtomicSortedColumns.factory()` is unconditionally selected for the mutable column container in `CollationController` and `Memtable`, but `AtomicSortedColumns` does not correctly handle the `addColumn` semantics required by super-column (and counter) types.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: wrong-collection-type-for-cf-variant
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`CollationController.collectTimeOrderedData` and `collectAllData`, as well as `Memtable.put`, always used `AtomicSortedColumns` as the mutable column container when `mutableColumns` was true. `AtomicSortedColumns` does not implement the nested-update semantics needed for super columns (where sub-column merging must happen inside a parent column that is itself mutable). Using it for super-column or counter families caused incorrect merge behavior and assertion failures. The fix dispatches on `cfs.metadata.cfType`: super-column families use the new `ThreadSafeSortedColumns` (backed by `ConcurrentSkipListMap`), while regular families continue to use `AtomicSortedColumns`.

## Example

```java
// BEFORE — AtomicSortedColumns always used, wrong for super columns
ISortedColumns.Factory factory = mutableColumns
    ? AtomicSortedColumns.factory()
    : TreeMapBackedSortedColumns.factory();
```

```java
// AFTER — dispatch on CF type
ISortedColumns.Factory factory = mutableColumns
    ? cfs.metadata.cfType == ColumnFamilyType.Super
        ? ThreadSafeSortedColumns.factory()
        : AtomicSortedColumns.factory()
    : TreeMapBackedSortedColumns.factory();
```
