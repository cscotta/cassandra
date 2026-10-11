# Bug 2673: Non-existent rows incorrectly cached

**Commit**: 7e40e39a4e · **Ticket**: CASSANDRA-2723

## Detection Rule
When reading from SSTables and no iterators are found (meaning no data exists at all for the row), return null immediately rather than returning an empty ColumnFamily that will be cached as a ghost entry.

## Code Smell
`getTopLevelColumns()` returning an empty `returnCF` object when `iterators.size() == 0`, which is then cached as a real (empty) row and prevents the bloom filter fast-path from correctly detecting that no row exists.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: empty-result-cached-as-row
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`ColumnFamilyStore.getTopLevelColumns()` distinguished between "no data at all" (should return `null` so bloom filter can be used on future reads) and "data existed but is now deleted" (should cache the empty CF). Without an early-return on empty `iterators`, an empty CF was returned for non-existent rows, filling the row cache with phantom entries and bypassing the bloom filter optimization on subsequent reads.

## Example
```java
// BEFORE
Comparator<IColumn> comparator = filter.filter.getColumnComparator(getComparator());
Iterator collated = IteratorUtils.collatedIterator(comparator, iterators);
filter.collectCollatedColumns(returnCF, collated, gcBefore);
return returnCF;
```
```java
// AFTER
if (iterators.size() == 0)
    return null;  // no data at all — don't cache
Comparator<IColumn> comparator = filter.filter.getColumnComparator(getComparator());
Iterator collated = IteratorUtils.collatedIterator(comparator, iterators);
filter.collectCollatedColumns(returnCF, collated, gcBefore);
return returnCF;
```
