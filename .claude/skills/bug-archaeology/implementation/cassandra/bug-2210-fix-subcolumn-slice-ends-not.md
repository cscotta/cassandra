# Bug 2210: Subcolumn Slice Finish Boundary Not Enforced

**Commit**: f02928f887 · **Ticket**: CASSANDRA-4826
**Subsystem**: src/java/org/apache/cassandra/db/filter/SliceQueryFilter.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

In `SliceQueryFilter.filterSuperColumn()`, the loop that advanced through subcolumns to find the start position used `Iterators.concat` to splice the first matching column back onto the front of the iterator, but it never checked the finish boundary. Columns beyond the requested `finish()` were silently passed through to `collectReducedColumns`, returning more data than requested. The fix replaces the ad-hoc concat pattern with a Guava `AbstractIterator` that both skips subcolumns before `start()` and stops iteration when the first subcolumn past `finish()` is encountered.

### Before / After

```java
// BEFORE
while (subcolumns.hasNext()) {
    IColumn column = subcolumns.next();
    if (comparator.compare(column.name(), start()) >= 0) {
        subcolumns = Iterators.concat(Iterators.singletonIterator(column), subcolumns);
        break;
    }
}
// no finish check -- all remaining subcolumns pass through
collectReducedColumns(scFiltered, subcolumns, gcBefore);
```

```java
// AFTER
Iterator<IColumn> results = new AbstractIterator<IColumn>() {
    protected IColumn computeNext() {
        while (subcolumns.hasNext()) {
            IColumn subcolumn = subcolumns.next();
            if (comparator.compare(subcolumn.name(), start()) < 0)
                continue;
            if (finish().remaining() > 0 && comparator.compare(subcolumn.name(), finish()) > 0)
                break;
            return subcolumn;
        }
        return endOfData();
    }
};
collectReducedColumns(scFiltered, results, gcBefore);
```

---

## How to Spot

Slice filtering code that only handles the lower bound (start) of a range without a corresponding upper bound (finish) check is a strong signal that the finish boundary will be ignored. When using an iterator-splicing pattern (`Iterators.concat`) to rewind after a scan, verify that a stop condition for the upper end of the range is also present.

---

## Test

No test added.
