# Bug 1815: Fix potential assertion error in AbstractQueryPager.discardFirst

**Commit**: a462083fc4 · **Ticket**: CASSANDRA-6447
**Subsystem**: src/java/org/apache/cassandra/service/pager/AbstractQueryPager.java
**Category**: Missing null / bounds check

---

## Root Cause

`discardFirst` assumed it could always discard exactly one live cell from the first row of the result list, and asserted `discarded == 1`. When the first row contained only a range tombstone (no live cells), the discard loop produced zero, failing the assertion. Similarly, `discardLast` could run off the beginning of the list if there were fewer live cells than requested. The fix generalises both methods to loop across multiple rows until the required number of cells have been discarded, and returns an empty list rather than asserting when there is less live data than needed.

### Before / After

```java
// BEFORE
Row first = rows.get(0);
ColumnFamily newCf = first.cf.cloneMeShallow();
int discarded = isReversed()
              ? discardLast(first.cf, 1, newCf)
              : discardFirst(first.cf, 1, newCf);
assert discarded == 1;  // fails when row has only tombstone
```

```java
// AFTER
while (toDiscard > 0 && i < rows.size())
{
    Row first = rows.get(i++);
    firstKey = first.key;
    firstCf = first.cf.cloneMeShallow();
    toDiscard -= isReversed()
               ? discardLast(first.cf, toDiscard, firstCf)
               : discardFirst(first.cf, toDiscard, firstCf);
}
if (i >= rows.size())
    return Collections.<Row>emptyList();
```

---

## How to Spot

Assertions of the form `assert discarded == N` on results from a discard loop are fragile when rows can contain only tombstones or when the dataset is smaller than expected. Replace hard assertions on counts with proper boundary checks and graceful empty-list returns.

---

## Test

No test added directly for this fix; the regression scenario is: page through a result set where the first (or last) page contains a row with only range tombstones and no live cells — the pager must not throw `AssertionError`.
