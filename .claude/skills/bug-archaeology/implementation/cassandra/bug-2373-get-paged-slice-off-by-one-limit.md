# Bug 2373: get_paged_slice ignores isPaging flag, re-counting start column against the limit

**Commit**: fc7e86404a · **Ticket**: CASSANDRA-4136

## Detection Rule

When a range-slice query is meant to "page" from a specific start column (exclusive), verify that the filter's column-counting logic treats the start column as already-consumed rather than counting it as part of the requested limit; otherwise the first page of results will be off-by-one.

## Code Smell

`ExtendedFilter.create(cfs, filter, clause, maxResults, maxIsColumns)` is called without an `isPaging` flag — the filter counts the start column (used for continuation) as one of the `maxResults`, so each subsequent page returns one fewer result than requested, and the final page may appear prematurely.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: paging-start-column-counted-in-limit
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`get_paged_slice` uses a start column as an exclusive cursor: results should begin *after* that column. However, the `ExtendedFilter` used for range-slice queries counted the start column as one result against the limit unless told it was in "paging" mode. The `isPaging` flag (and its companion `RangeSliceCommand` field with serialization support) was added so that `ExtendedFilter` can adjust its `updateFilter` logic to skip the first already-seen column from the count. Without this flag, each page silently returned `limit - 1` live columns.

## Example

```java
// BEFORE
getRangeSlice(..., maxResults, columnFilter, rowFilter, true /* maxIsColumns */);
// ExtendedFilter has no isPaging concept; start column counts toward maxResults
```

```java
// AFTER
getRangeSlice(..., maxResults, columnFilter, rowFilter,
              true /* maxIsColumns */, false /* isPaging — set true for get_paged_slice */);
// ExtendedFilter.updateFilter() adjusts count when isPaging=true
```
