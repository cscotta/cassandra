# Bug 1711: Fix merge

**Commit**: 47675ce759 · **Ticket**: n/a
**Subsystem**: db/filter/SliceQueryFilter
**Category**: Logic error in condition

---

## Root Cause

A bad merge introduced two problems into the tombstone-threshold warning path of `SliceQueryFilter`: the local variable `type` was declared as `AbstractType<?>` (a raw cell comparator) instead of `CellNameType` (the composite-aware type that knows how to stringify composite cell names), and a null check `if (sl == null) continue` guarded an iteration where null slices should never exist. Using the wrong type would either fail to compile or produce garbled output in the warning log message; the null guard masked a logic error rather than protecting against a legitimate case.

### Before / After

```java
// BEFORE
AbstractType<?> type = container.metadata().comparator;
for (ColumnSlice sl : slices)
{
    if (sl == null)
        continue;
    sb.append(type.getString(sl.start));
```

```java
// AFTER
CellNameType type = container.metadata().comparator;
for (ColumnSlice sl : slices)
{
    assert sl != null;
    sb.append(type.getString(sl.start));
```

---

## How to Spot

After resolving a merge conflict, verify that type declarations in the merged region match the type system in the target branch — a variable re-declared with a parent type compiles fine but silently loses subtype behavior.

---

## Test

No test added.
