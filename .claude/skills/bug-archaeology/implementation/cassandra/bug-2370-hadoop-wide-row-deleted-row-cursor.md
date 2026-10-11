# Bug 2370: Hadoop wide-row iterator loses position after reading a deleted row

**Commit**: b184972b0f · **Ticket**: CASSANDRA-4154

## Detection Rule

When a paged iterator advances by re-querying with a "last seen column" start key, verify that the "last seen" value is updated for every row returned — including rows that contain only tombstones — not only on the initial query or on non-empty rows.

## Code Smell

`startColumn` is initialised to `EMPTY_BYTE_BUFFER` at the beginning of each `maybeInit()` call and only updated from `Iterables.getLast(lastRow.columns)` — if `lastRow` was a deleted/empty row, `startColumn` reverts to empty, causing the next page request to restart from the beginning of the token range.

## Tags

- scope: cassandra
- category: state-not-cleaned
- code-pattern: cursor-not-updated-on-empty-row
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

`WideRowIterator` used a `startColumn` field to resume paged queries. On each `maybeInit()` call it reset `startColumn = EMPTY_BYTE_BUFFER`, then conditionally set it to the last column of the previous row. If the last row visited had been deleted (empty columns), `Iterables.getLast(lastRow.columns)` would throw or the branch would be skipped, and `startColumn` would remain empty — the next page would re-read from the start of the split. The fix introduces a persistent `lastColumn` field that is updated in `computeNext()` after every successful row is returned, so deleted or empty intermediate rows do not reset the cursor.

## Example

```java
// BEFORE (maybeInit)
startColumn = ByteBufferUtil.EMPTY_BYTE_BUFFER; // reset every time
// ...
startColumn = Iterables.getLast(lastRow.columns).column.name; // skipped for deleted rows
rows = client.get_paged_slice(cfName, keyRange, startColumn, ...);
```

```java
// AFTER
private ByteBuffer lastColumn = ByteBufferUtil.EMPTY_BYTE_BUFFER; // persists across pages
// in computeNext():
Pair<...> next = wideColumns.next();
lastColumn = next.right.values().iterator().next().name(); // updated every row
return next;
// maybeInit uses lastColumn directly, never resets to empty
```
