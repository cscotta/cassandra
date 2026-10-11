# Bug 2693: CQL > and < operators in range slices include boundary rows

**Commit**: 8d3922771b · **Ticket**: CASSANDRA-2592

## Detection Rule
When CQL translates a strict inequality range (`>` or `<`) into a storage-layer range slice that is inherently inclusive, fetch one extra row and then post-filter to drop the boundary row if it matches the excluded key.

## Code Smell
`multiRangeSlice()` building an `AbstractBounds` from the start/finish keys and passing `select.getNumRecords()` directly to `getRangeSlice()`, without accounting for the `>` vs `>=` or `<` vs `<=` distinction, and without post-filtering the boundary rows.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: inclusive-bounds-exclusive-operator
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
The internal range-slice API uses inclusive bounds (`Bounds`). When CQL specified `> startKey`, the query was executed as `>= startKey`, so the row at `startKey` appeared in the result set. Similarly, `< finishKey` included the finish-key row. The fix fetches `numRecords + 1` rows when a start key is present, then strips the boundary rows if they match an exclusive bound, and finally trims the result to `numRecords`.

## Example
```java
// BEFORE
rows = StorageProxy.getRangeSlice(new RangeSliceCommand(..., bounds, select.getNumRecords()), ...);
return rows;
```
```java
// AFTER
int limit = select.isKeyRange() && select.getKeyStart() != null
          ? select.getNumRecords() + 1 : select.getNumRecords();
rows = StorageProxy.getRangeSlice(new RangeSliceCommand(..., bounds, limit), ...);
if (select.getKeyStart() != null && !select.includeStartKey())
    if (rows.get(0).key.key.equals(startKey)) rows.remove(0);
if (select.getKeyFinish() != null && !select.includeFinishKey())
    if (rows.get(rows.size()-1).key.key.equals(finishKey)) rows.remove(rows.size()-1);
return rows.subList(0, Math.min(select.getNumRecords(), rows.size()));
```
