# Bug 1519: Fix UDT field selection with empty field

**Commit**: fa8e5158a5 · **Ticket**: CASSANDRA-7670
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Missing null / bounds check

---

## Root Cause

`FieldSelector.compute()` in `Selection.java` called `type.split(selected.compute(rs))` without checking whether the computed UDT value was `null`. When a UDT column contains an empty (null) field and is selected in a CQL query, `selected.compute(rs)` returns `null`, causing `type.split(null)` to throw a `NullPointerException`. The fix adds an explicit null check that short-circuits to return `null` before attempting to split the value.

### Before / After

```java
// BEFORE
public ByteBuffer compute(ResultSetBuilder rs) throws InvalidRequestException
{
    ByteBuffer[] buffers = type.split(selected.compute(rs));
    return field < buffers.length ? buffers[field] : null;
}
```

```java
// AFTER
public ByteBuffer compute(ResultSetBuilder rs) throws InvalidRequestException
{
    ByteBuffer value = selected.compute(rs);
    if (value == null)
        return null;
    ByteBuffer[] buffers = type.split(value);
    return field < buffers.length ? buffers[field] : null;
}
```

---

## How to Spot

Whenever a `compute()` or similar method result is immediately passed as an argument to another method, verify whether it can return `null`. UDT field selectors are particularly susceptible because individual UDT fields may be absent (null) even when the enclosing UDT row is present.

---

## Test

No test added.
