# Bug 2439: get_count over-counts columns due to incorrect paging boundary handling

**Commit**: a5b3abb667 · **Ticket**: CASSANDRA-3798

## Detection Rule

When paginating through a slice and using the last column of one page as the start of the next, verify that the overlap column is counted exactly once and that the termination condition accounts for a non-full final page.

## Code Smell

A paging loop adjusts `totalCount--` and `requestedCount++` to compensate for repeated boundary columns, but the logic misidentifies which column is duplicated and fails to terminate early when the last page is smaller than the page size.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: paging-boundary-double-count
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause

The old `get_count` paging loop used the last column of each page as the start of the next page, then tried to undo the double-count with `totalCount--; requestedCount++`. The undo logic was applied at the wrong point and did not handle the case where the very first column of the new page differed from the boundary (i.e., no overlap). It also continued paging when the last page came back full even if `requestedCount` was satisfied, causing over-counting. The fix tracks whether the first column of each follow-on page equals the previous end column, adds only the genuinely new columns, and stops when a page is not full.

## Example

```java
// BEFORE – undo logic applied too late, can over-count
totalCount += columns.size();
requestedCount -= columns.size();
if (requestedCount == 0 || (columns.size() == 1 && lastName.equals(start))) break;
else { predicate.start = lastName; totalCount--; requestedCount++; }

// AFTER – count only new columns, stop on partial page
ByteBuffer firstName = getName(columns.get(0));
int newColumns = pages == 0 || !firstName.equals(predicate.start)
                 ? columns.size() : columns.size() - 1;
totalCount += newColumns;
requestedCount -= newColumns;
pages++;
if (requestedCount == 0 || columns.size() < predicate.slice_range.count) break;
else predicate.start = getName(columns.get(columns.size() - 1));
```
