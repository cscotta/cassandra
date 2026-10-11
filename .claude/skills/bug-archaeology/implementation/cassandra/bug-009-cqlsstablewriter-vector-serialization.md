# Bug 009: Fix CQLSSTableWriter serialization of vector of date and time

**Commit**: 0136fc9c8f · **Ticket**: CASSANDRA-20979
**Subsystem**: CQL / SSTable I/O / Type System
**Category**: Wrong constant / default value

---

## Root Cause

`CQLSSTableWriter` uses `TypeCodec` implementations to serialize CQL values into SSTable binary format. The `DateCodec` had an explicit `serializedSize()` override returning `8`, treating `date` as a fixed-width 8-byte type, even though `SimpleDateType` in Cassandra uses a 4-byte unsigned integer. This over-allocation caused data corruption when dates were embedded in vector columns, where the codec's reported size is used to compute element offsets. Additionally, `TimeCodec` was missing a `serializedSize()` override entirely and inherited the base class default, whereas `TimeType` is declared as variable-length; this mismatch similarly broke vector serialization for `time` columns.

### Before / After

```java
// BEFORE — DateCodec incorrectly reported 8-byte fixed size
public class DateCodec extends TypeCodec<LocalDate> {
    @Override
    public int serializedSize()
    {
        return 8;  // wrong: SimpleDateType is 4 bytes, not 8
    }
}

// BEFORE — TimeCodec had no serializedSize() override, inherited wrong default

// AFTER — DateCodec override removed (inherits correct behavior from parent)
// (the erroneous override is simply deleted)

// AFTER — TimeCodec explicitly matches TimeType's variable-length declaration
public class TimeCodec extends TypeCodec<Long> {
    @Override
    public int serializedSize()
    {
        // matching behavior of TimeType, which is not declared as fixed length
        return VARIABLE_LENGTH;
    }
}
```

---

## How to Spot

Whenever a `TypeCodec` subclass overrides `serializedSize()`, verify that the returned constant matches the on-disk byte width used by the corresponding internal `AbstractType`. A mismatch is especially harmful inside vector types because the element size is used to calculate offsets for every element in the fixed-layout vector buffer. Cross-check the codec's declared size against the `AbstractType.valueLengthIfFixed()` (or equivalent) and against the actual `serialize()`/`deserialize()` byte count in unit tests that read back written values.

---

## Test

`CQLSSTableWriterTest.testWritingVectorData` writes 100 rows containing `VECTOR<DATE, 5>` and `VECTOR<TIME, 5>` columns via `CQLSSTableWriter`, then reads them back via `QueryProcessor.executeInternal` and asserts that every element in each vector round-trips to its original value with the correct Java type (`Integer` for date, `Long` for time).
