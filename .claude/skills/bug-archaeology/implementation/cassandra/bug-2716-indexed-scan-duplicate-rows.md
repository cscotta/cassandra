# Bug 2716: Indexed scan returns duplicate rows when restart key equals the last visited key

**Commit**: b553e77331 · **Ticket**: CASSANDRA-2406

## Detection Rule
When implementing a paginated scan that resumes from the last-seen key, verify that the first result of the new page is not a duplicate of the last result of the previous page.

## Code Smell
`CFS.scan` sets `startKey = dataKey` at the end of each iteration but never records the previous `startKey`; on the next outer-loop pass the first column read is exactly `dataKey` again and passes the `range.contains()` check, producing a duplicate row.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: paginated-scan-duplicate-start
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause
The indexed-range scan loop advances by setting `startKey = dataKey` after each batch. On the next iteration the index scan resumes from `startKey`, which is the same row just returned, so that row is emitted again. The fix tracks `lastDataKey` and adds `|| dataKey.equals(lastDataKey)` to the skip condition, so the first row of each resumed page is skipped when it matches the previous page's last row.

## Example
```java
// BEFORE — duplicate first row on resumed page
if (!range.contains(dk.token))
    continue;
// ... emit row ...
startKey = dataKey;
```
```java
// AFTER — skip row if it matches the last-seen key
if (!range.contains(dk.token) || dataKey.equals(lastDataKey))
    continue;
// ... emit row ...
lastDataKey = startKey = dataKey;
```
