# Bug 2169: Wrong Class Type Assertion Error in CounterColumn.diff()

**Commit**: d0292ef45a · **Ticket**: CASSANDRA-4976
**Subsystem**: src/java/org/apache/cassandra/db/CounterColumn.java
**Category**: Missing null / bounds check

---

## Root Cause

`CounterColumn.diff()` asserted that the incoming column was always a `CounterColumn`, but the method can legally receive a `DeletedColumn` (tombstone) as input because `reconcile()` — which calls `diff()` — already handles tombstones. When a `DeletedColumn` arrived whose timestamp was not greater than the `CounterColumn`'s, the assertion fired even though that is a valid state. The fix widens the assertion to permit `DeletedColumn` as input, then adds a second assert after the timestamp check to verify that a `DeletedColumn` cannot survive past that point (since a surviving tombstone would mean its timestamp is higher, which is already handled).

### Before / After

```java
// BEFORE
assert column instanceof CounterColumn : "Wrong class type.";
if (timestamp() < column.timestamp())
    return column;
if (timestampOfLastDelete() < ((CounterColumn)column).timestampOfLastDelete())
```

```java
// AFTER
assert (column instanceof CounterColumn) || (column instanceof DeletedColumn) : "Wrong class type: " + column.getClass();
if (timestamp() < column.timestamp())
    return column;
assert !(column instanceof DeletedColumn) : "Wrong class type: " + column.getClass();
if (timestampOfLastDelete() < ((CounterColumn)column).timestampOfLastDelete())
```

---

## How to Spot

When an assertion checks class identity in a merge/diff method, verify that all legal input types produced by callers are covered. If other code paths can legally produce a different subtype, the assertion must be widened (and a subsequent cast narrowed with a secondary guard).

---

## Test

No test added.
