# Bug 0196: VectorType Missing getMaskedValue Override Breaks DDM mask_default Function

**Commit**: 3b9881bfa6 · **Ticket**: CASSANDRA-18889
**Subsystem**: db/marshal/VectorType, cql3/CQL3Type (Vector)
**Category**: Missing override of default method

---

## Root Cause

`VectorType` did not override `getMaskedValue()` from `AbstractType`, so the base implementation was used. The base `getMaskedValue()` is only valid for scalar types and does not produce a value that can be validated and round-tripped as a vector. When DDM's `mask_default` function was applied to a vector column, it called `getMaskedValue()` on the column type and received an invalid result (or threw), making it impossible to use `DEFAULT` or `mask_default()` column masking on vector columns. Additionally, `CQL3Type.Raw.Vector` was missing `equals`/`hashCode` overrides, which could cause incorrect deduplication when vector types were used as map keys or stored in sets.

### Before / After

```java
// BEFORE – no override in VectorType; base AbstractType.getMaskedValue() invoked
```

```java
// AFTER
@Override
public ByteBuffer getMaskedValue()
{
    List<ByteBuffer> values = Collections.nCopies(dimension, elementType.getMaskedValue());
    return serializer.serializeRaw(values, ByteBufferAccessor.instance);
}
```

---

## How to Spot

Every concrete `AbstractType` subclass that represents a composite (vector, tuple, UDT) must override `getMaskedValue()` to produce a structurally valid composite value. If the base implementation returns a scalar zero/null buffer, the composite serializer will reject it during validation. Review all composite type classes and confirm the method is overridden and that its return value passes `type.validate()`.

---

## Test

`MaskingFunctionTester.testMaskingOnVector` applies default and replace masking functions to `vector<int,2>`, `vector<float,2>`, and `vector<text,2>` columns and asserts the masked values match expected defaults; `AbstractTypeTest.maskedValue` property-checks that every type's `getMaskedValue()` round-trips through compose/decompose.
