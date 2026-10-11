# Bug 2244: ORDER BY Column Position Resolved from Stale Field Instead of Current Selection

**Commit**: a4c397b49c · **Ticket**: CASSANDRA-4689
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectStatement.orderResults()` needed to find the index of each ORDER BY column within the projected result row. It called `getColumnPositionInSelect(ordering)`, which scanned a field `selectedNames` on the statement object. However, when "extended selections" (e.g., `SELECT *` or function-based columns) were used, the actual selection list differed from the cached `selectedNames` field, causing the wrong column index to be returned or an `ArrayIndexOutOfBoundsException`. The fix passes the live `selection` list computed during query execution as an explicit parameter, ensuring the position is resolved against the real, current column set.

### Before / After

```java
// BEFORE — orderResults called without the live selection list
orderResults(cqlRows);
// ...
private void orderResults(List<CqlRow> cqlRows) { ... }
private int getColumnPositionInSelect(CFDefinition.Name columnName) {
    for (int i = 0; i < selectedNames.size(); i++) {
        if (selectedNames.get(i).left.equals(columnName)) return i;
    }
```

```java
// AFTER — selection list threaded through explicitly
orderResults(selection, cqlRows);
// ...
private void orderResults(List<Pair<CFDefinition.Name, Selector>> selection, List<CqlRow> cqlRows) { ... }
private int getColumnPositionInSelect(List<Pair<CFDefinition.Name, Selector>> selection, CFDefinition.Name columnName) {
    for (int i = 0; i < selection.size(); i++) {
        if (selection.get(i).left.equals(columnName)) return i;
    }
```

---

## How to Spot

When a sort/ordering helper resolves column positions using a pre-cached field rather than the actual selection computed at query time, extended or dynamic selections will produce incorrect sort orders or index errors. Always pass the live selection list as a parameter rather than reading from object state that may be stale.

---

## Test

No test added.
