# Bug 0604: NPE Comparing Against Null Frozen Collection in LWT

**Commit**: 2c150980cc · **Ticket**: CASSANDRA-14087
**Subsystem**: `src/java/org/apache/cassandra/cql3/ColumnCondition.java`
**Category**: Missing null / bounds check

---

## Root Cause

`ColumnCondition` evaluated LWT IF conditions by looking up a `Cell` for the target column. When the target column was a frozen collection and its value was stored as null in the row, no cell existed (`cell == null`). The existing null-handling branch only covered the case where the condition value was null (`conditionValue == null`). When the condition value was non-null but the cell was null, code fell through to `cell.value()` without a null check, causing a `NullPointerException`. The fix adds an explicit branch: if the cell is null (the stored value is null) but the condition expects a non-null value, the condition simply evaluates to `false`.

### Before / After

```java
// BEFORE — null conditionValue handled, but null cell falls through to NPE
if (conditionValue == null)
{
    if (operator == Operator.EQ)
        return cell == null || cell.isTombstone() || ...;
    else
        throw new InvalidRequestException(...);
}
// no check for cell == null when conditionValue != null
ByteBuffer storedValue = cell.value(); // NPE when cell is null
```

```java
// AFTER — null cell with non-null condition returns false
if (conditionValue == null)
{
    ...
}
else if (cell == null) // cell is null but condition has a value
{
    return false;
}
ByteBuffer storedValue = cell.value(); // safe
```

---

## How to Spot

In LWT condition evaluation, the pairing of `(conditionValue, cell)` produces a 2×2 matrix: both null, conditionValue null only, cell null only, both present. Code that handles the `conditionValue == null` axis without a corresponding guard on the `cell == null` axis will NPE when a stored value is absent but the condition operand is non-null.

---

## Test

`testFrozenWithNullValues` inserts a null frozen list/map/set, then asserts that `UPDATE ... IF m = <non-null>` returns `[false, null]` without throwing.
