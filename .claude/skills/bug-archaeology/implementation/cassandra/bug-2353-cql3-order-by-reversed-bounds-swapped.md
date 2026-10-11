# Bug 2353: CQL3 ORDER BY DESC swaps slice start/end, returning wrong or empty results

**Commit**: 1686a36ed5 · **Ticket**: CASSANDRA-4160

## Detection Rule

When a query layer builds a `SliceRange` or `SlicePredicate` for a reversed scan, verify that `start` is populated from the logical END bound and `finish` from the logical START bound — reversed comparators require physically swapped byte-buffer boundaries.

## Code Smell

A method that sets `sliceRange.start = getRequestedBound(Bound.START, ...)` and `sliceRange.finish = getRequestedBound(Bound.END, ...)` without checking `isColumnsReversed` will issue an inverted range query when ORDER BY DESC is used, returning an empty or wrong result set.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: reversed-range-bounds-not-swapped
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause

`SelectStatement` built `SliceRange.start` and `SliceRange.finish` by always fetching `Bound.START` and `Bound.END` respectively, regardless of whether the query requested reversed column order. A reversed (`ORDER BY ... DESC`) scan must send the logical end bound as the physical start and vice versa, because the storage engine traverses columns in reverse byte order. Both the CQL path (row iteration) and the Thrift path (slice predicate) had this bug. The fix conditionally swaps the bound lookup when `parameters.isColumnsReversed` is true.

## Example

```java
// BEFORE
sliceRange.start  = getRequestedBound(Bound.START, variables);
sliceRange.finish = getRequestedBound(Bound.END,   variables);
sliceRange.reversed = parameters.isColumnsReversed;
```

```java
// AFTER
sliceRange.start  = getRequestedBound(
        parameters.isColumnsReversed ? Bound.END   : Bound.START, variables);
sliceRange.finish = getRequestedBound(
        parameters.isColumnsReversed ? Bound.START : Bound.END,   variables);
sliceRange.reversed = parameters.isColumnsReversed;
```
