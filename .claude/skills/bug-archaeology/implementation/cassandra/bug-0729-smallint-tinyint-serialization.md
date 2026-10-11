# Bug 0729: SmallInt and TinyInt serialization in arithmetic operations

**Commit**: f5e0a7cdba · **Ticket**: CASSANDRA-11935
**Subsystem**: CQL / Type System
**Category**: Wrong serialization / deserialization

---

## Root Cause
`ByteType` (tinyint) and `ShortType` (smallint) each overrode `valueLengthIfFixed()` to return `1` and `2` respectively, advertising themselves as fixed-length types. However, the codec infrastructure did not treat them as truly fixed-length in all paths; `OperationFcts.getValueLength()` called `type.valueLengthIfFixed()` but the result was only trusted when positive. Because the type's general size query returned the correct value, arithmetic operation functions that called `getValueLength()` to size their output buffers took the wrong branch (the `isCounter()` fallback) and produced incorrectly sized buffers. The fix removes the `valueLengthIfFixed()` overrides from `ByteType` and `ShortType` (making them behave as variable-length like the rest of the numeric hierarchy) and instead adds explicit `tinyint`/`smallint` special-cases at the top of `getValueLength()` in `OperationFcts`.

### Before / After
```java
// BEFORE — ByteType.java (and analogously ShortType.java)
@Override
public int valueLengthIfFixed()
{
    return 1;  // advertised as fixed, but caused wrong path in OperationFcts
}

// AFTER — overrides removed from ByteType and ShortType entirely

// AFTER — OperationFcts.getValueLength()
if (type == ByteType.instance)
    return 1;
if (type == ShortType.instance)
    return 2;
```

---

## How to Spot
When a new numeric type is added to Cassandra, be careful about where `valueLengthIfFixed()` is consumed. If any consumer has a fallback path that is triggered when `valueLengthIfFixed()` returns a non-positive sentinel (e.g., `-1`) and the new type only partially overrides the method, the fallback may silently activate. Arithmetic and aggregation functions are especially sensitive because they pre-allocate output buffers based on the declared fixed length.

---

## Test
No new test added in this commit; the fix is a followup to the original CASSANDRA-11935 change.
