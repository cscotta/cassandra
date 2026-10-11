# Bug 0982: List Element Path Resolved in Wrong Branch

**Commit**: d2c41b1b3c · **Ticket**: n/a
**Subsystem**: CQL / List operations
**Category**: Logic error in condition

---

## Root Cause
In `Lists.Setter.execute()`, the code that applies a by-index list update resolves the target cell path (`elementPath`) and then either adds a tombstone (when `value == null`) or adds a new cell (when `value` is not unset). In the broken version, `elementPath` was declared and retrieved inside the `else if (value != UNSET_BYTE_BUFFER)` branch, but the `if (value == null)` tombstone branch that preceded it also used `elementPath`. This meant that when `value` was `null`, `elementPath` was referenced before it was assigned (it was declared elsewhere and would be `null`), causing a `NullPointerException` when attempting to add the tombstone.

The fix moves the `elementPath` resolution to before the `if/else` branches so it is always available regardless of which branch executes.

### Before / After
```java
// BEFORE – elementPath only resolved in the else-if branch
if (value == null)
{
    params.addTombstone(column, elementPath);  // elementPath is null here
}
else if (value != ByteBufferUtil.UNSET_BYTE_BUFFER)
{
    CellPath elementPath = existingRow.getComplexColumnData(column).getCellByIndex(idx).path();
    params.addCell(column, elementPath, value);
}

// AFTER – elementPath resolved before the branches
CellPath elementPath = existingRow.getComplexColumnData(column).getCellByIndex(idx).path();
if (value == null)
    params.addTombstone(column, elementPath);
else if (value != ByteBufferUtil.UNSET_BYTE_BUFFER)
    params.addCell(column, elementPath, value);
```

---

## How to Spot
When a variable is used in multiple branches of an if/else chain, it must be declared and initialised before the chain, not inside one of its branches. Code review should flag any variable that appears in an `if` branch but is only assigned in a later `else if` branch. The compile-time "variable might not have been initialized" warning would normally catch this, but is suppressed when the variable was declared in an outer scope and left uninitialised.

---

## Test
No test added (this was a build/compile fix).
