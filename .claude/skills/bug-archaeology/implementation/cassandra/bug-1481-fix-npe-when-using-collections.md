# Bug 1481: Fix NPE when using collections in frozen tuple

**Commit**: cf76ac66af · **Ticket**: CASSANDRA-7869
**Subsystem**: cql3 / type system
**Category**: Missing null / bounds check

---

## Root Cause

`CQL3Type.Raw.Collection.freeze()` unconditionally called `keys.freeze()` without checking whether `keys` was null. For collection types that have no key component (lists and sets), the `keys` field is never initialised and remains null. Calling `freeze()` on a null reference produced a NullPointerException whenever a list or set was used as a component inside a `frozen<tuple<...>>`.

### Before / After

```java
// BEFORE
public Raw freeze()
{
    keys.freeze();   // NPE when keys == null (lists, sets)
    values.freeze();
    return super.freeze();
}
```

```java
// AFTER
public Raw freeze()
{
    if (keys != null)
        keys.freeze();
    values.freeze();
    return super.freeze();
}
```

---

## How to Spot

Whenever a method is called on a field that is only populated for some subtypes (map vs. list/set), add a null guard. Look for fields initialised conditionally in factory/builder methods but used unconditionally in shared helpers like `freeze()`.

---

## Test

`TupleTypeTest.testFrozenTupleWithList/Set/Map`: creates a table with a `frozen<tuple<text, list<int>>>` (and analogous set/map variants), inserts rows, and asserts the round-tripped values are correct. Without the fix, insert preparation throws NPE at the freeze step.
