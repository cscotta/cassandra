# Bug 0743: ColumnCounter::countAll iterates forward for reverse queries, returning wrong cell count

**Commit**: 3f45010774 · **Ticket**: CASSANDRA-13222
**Subsystem**: Storage engine / query paging
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`ColumnCounter.countAll()` always iterated over a `ColumnFamily` in forward order using `container.iterator()`. Its subclass `GroupByPrefixReversed` — used for `ORDER BY` DESC queries — overrode `count(Cell, tester)` to track prefixes in reverse, but `countAll()` in the base class still fed cells in forward order. The `InOrderDeletionTester` that detects whether a cell is already deleted is order-sensitive; presenting cells out of order caused it to skip deletions or count cells that should have been filtered, producing wrong row counts for paged reverse queries. The fix adds an overridable `getCellIterator(ColumnFamily)` hook that `GroupByPrefixReversed` overrides to return `container.reverseIterator()`, so the deletion tester always sees cells in the direction the subclass expects.

### Before / After
```java
// BEFORE — always forward, wrong for reverse subclass
for (Cell c : container)
    count(c, tester);

// AFTER — direction chosen by subclass
Iterator<Cell> cells = getCellIterator(container);
while (cells.hasNext())
    count(cells.next(), tester);

// GroupByPrefixReversed override:
@Override
public Iterator<Cell> getCellIterator(ColumnFamily container) {
    return container.reverseIterator();
}
```

---

## How to Spot
When a base class iterates a collection and a subclass changes the semantics of order-sensitive processing, the iteration direction should be a virtual method rather than hard-coded. In review, look for `for (X x : container)` in base classes where a subclass is known to operate in a different traversal order, and check whether any downstream logic (e.g., an `InOrderTester`) depends on traversal direction.

---

## Test
`QueryPagerTest` was extended with a new test covering paged reverse queries to verify that `countAll` returns the correct cell count for DESC-order clustering columns.
