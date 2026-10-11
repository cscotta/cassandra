# Bug 2444: Secondary index searcher compares IColumn object to ByteBuffer start key

**Commit**: 2f879f278d · **Ticket**: CASSANDRA-3786

## Detection Rule

When skipping a boundary element in a range scan (e.g., the exclusive start key), verify the comparison is between the element's key/name field and the boundary value, not the element object itself against a different type.

## Code Smell

`firstColumn.equals(startKey)` where `firstColumn` is an `IColumn` instance and `startKey` is a `ByteBuffer`; the types differ so equality is always false and the exclusive start key is never skipped.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: wrong-field-compared
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause

`KeysSearcher` tries to skip the first result when it equals the exclusive range start. The check used `firstColumn.equals(startKey)`, comparing an `IColumn` to a `ByteBuffer`. Since there is no cross-type equality contract, this was always false, so the exclusive boundary element was always included in results. The fix compares the column's name bytes: `firstColumn.name().equals(startKey)`.

## Example

```java
// BEFORE – IColumn never equals ByteBuffer, boundary never skipped
else if (range instanceof Range && indexColumns.hasNext() && firstColumn.equals(startKey))

// AFTER
else if (range instanceof Range && indexColumns.hasNext() && firstColumn.name().equals(startKey))
```
