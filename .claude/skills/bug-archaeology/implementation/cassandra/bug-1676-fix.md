# Bug 1676: ABSC.SearchIterator#next() Returns Wrong Cell When Name Precedes Current Position

**Commit**: 333986428e · **Ticket**: CASSANDRA-6933
**Subsystem**: db/ArrayBackedSortedColumns
**Category**: Logic error in condition

---

## Root Cause

The `SearchIterator` inside `ArrayBackedSortedColumns` was optimized for sequential access (CASSANDRA-6933): it tracks the current array index `i` and advances it on each call. The fix for that ticket compared `name` to `cells[i].name()` but only handled the equality case. If the requested `name` is less than `cells[i].name()` — i.e., the caller asked for a column that should appear *before* the current scan position — the iterator advanced `i` past the current cell, corrupting the position for subsequent calls and returning the wrong cell. The correct behaviour for `name < cells[i]` is to return `null` immediately (column not found), which is what the fix does by testing `c <= 0` and branching on the sign.

### Before / After

```java
// BEFORE
if (metadata.comparator.compare(name, cells[i].name()) == 0)
    return cells[i++];
```

```java
// AFTER
int c = metadata.comparator.compare(name, cells[i].name());
if (c <= 0)
    return c < 0 ? null : cells[i++];
```

---

## How to Spot

In a sorted-array iterator that caches its position, any comparison that only handles `== 0` (equality) and falls through for all other cases will misbehave when the requested key is smaller than the cached position. Always handle all three cases (`< 0`, `== 0`, `> 0`) when interpreting a comparator result.

---

## Test

No test added.
