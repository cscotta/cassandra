# Bug 1790: Fix Paging discardFirst IllegalArgumentException

**Commit**: 55211bca79 · **Ticket**: CASSANDRA-6555
**Subsystem**: src/java/org/apache/cassandra/service/pager
**Category**: Off-by-one

---

## Root Cause

`AbstractQueryPager.discardFirst` and `discardLast` each had two interrelated off-by-one errors. First, neither method guarded against an empty input list, so iterating on an empty `rows` list dereferenced an uninitialised `firstCf`/`lastCf`, causing `IllegalArgumentException`. Second, when the partially-consumed boundary row (`firstCf`/`lastCf`) still had columns remaining, the new-list-size calculation used `rows.size() - i` / `i + 1` instead of correctly accounting for whether the boundary row itself should be included, resulting in an undersized `ArrayList` allocation that led to an `IllegalArgumentException` from `ArrayList.subList`. The fix adds an early `rows.isEmpty()` guard and corrects the size arithmetic so the boundary row is conditionally counted.

### Before / After

```java
// BEFORE — discardFirst
if (toDiscard == 0)
    return rows;
// ...after loop...
if (toDiscard > 0 && i >= rows.size())
    return Collections.<Row>emptyList();
int count = firstCf.getColumnCount();
int newSize = rows.size() - i;
List<Row> newRows = new ArrayList<Row>(count == 0 ? newSize-1 : newSize);
```

```java
// AFTER — discardFirst
if (toDiscard == 0 || rows.isEmpty())
    return rows;
// ...after loop...
if (toDiscard > 0)
    return Collections.<Row>emptyList();
int count = firstCf.getColumnCount();
int newSize = rows.size() - (count == 0 ? i : i - 1);
List<Row> newRows = new ArrayList<Row>(newSize);
```

---

## How to Spot

Any method that iterates over a list to discard elements and then uses the loop counter to compute a new list size must handle the empty-list case explicitly and carefully track whether the loop's boundary element is included in the count.

---

## Test

`AbstractQueryPagerTest.DiscardFirstTest` and `DiscardLastTest` — drive `discardFirst`/`discardLast` through a sequence of incremental discard calls on a list of rows with varying column counts and assert the exact remaining rows and columns after each step.
