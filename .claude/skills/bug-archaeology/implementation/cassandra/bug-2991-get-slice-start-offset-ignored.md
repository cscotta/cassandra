# Bug 2991: get_slice ignores the "start" offset parameter, always returning from the beginning

**Commit**: 3c6fcf8d65 · **Ticket**: CASSANDRA-81

## Detection Rule
When a slice filter accepts both a count limit and a start offset, verify that the offset is applied before decrementing the count, and that both are wired through from the API handler down to the filter constructor.

## Code Smell
The call site instantiates `CountFilter(count)` without passing the `start` offset, so `CountFilter` always starts collecting from the first column regardless of the caller-supplied start index.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: parameter-not-forwarded
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
`Table.getRow(key, cf, start, count)` created `new CountFilter(count)`, silently ignoring the `start` parameter. `CountFilter` had no concept of an offset, so it always returned columns starting from position 0. The fix adds an `offset_` field to `CountFilter` with a two-arg constructor, decrements the offset counter before adding each column, and updates the call site to pass `start` to `CountFilter(count, start)`.

## Example
```java
// BEFORE: start parameter silently ignored
ColumnFamily columnFamily = cfStore.getColumnFamily(key, cf, new CountFilter(count));

// AFTER: start offset passed to filter
filteredCf = new CountFilter(count, start).filter(cf, columnFamily);

// CountFilter fix
if (offset_ <= 0) {
    filteredCf.addColumn(column);
    countLimit_--;
} else {
    offset_--;
}
```
