# Bug 1521: Fix UDT Field Selection with Empty Field

**Commit**: 1adc39f171 · **Ticket**: CASSANDRA-7670
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Missing null / bounds check

---

## Root Cause

`FieldSelector.compute()` called `type.split(selected.compute(rs))` unconditionally. When the selected UDT value itself was `null` (an empty or unset UDT field), `type.split(null)` propagated into downstream logic rather than short-circuiting. The fix adds an explicit null guard that returns `null` immediately when the computed UDT value is null, before attempting to split its fields.

### Before / After

```java
// BEFORE — NPE/incorrect result when UDT value is null
public ByteBuffer compute(ResultSetBuilder rs) throws InvalidRequestException
{
    ByteBuffer[] buffers = type.split(selected.compute(rs));
    return field < buffers.length ? buffers[field] : null;
}
```

```java
// AFTER — null UDT value is propagated cleanly
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

Any time a method chains `.split()` or similar deserialization on the result of a `compute()`/`evaluate()` call without a null check, it will blow up when the column or nested value is unset. Look for "intermediate result used directly as argument" patterns inside selector/evaluator hierarchies.

---

## Test

No dedicated test added in this commit. The fix is covered by the functional scenario: `SELECT udt_col.field FROM t` where the UDT column contains a null/empty value should return `null` rather than throwing.
