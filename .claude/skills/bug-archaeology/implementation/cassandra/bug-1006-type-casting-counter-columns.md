# Bug 1006: Type Casting Fails for Counter Columns

**Commit**: 485c23d29b · **Ticket**: CASSANDRA-10824
**Subsystem**: CQL / type system / functions
**Category**: Wrong serialization / deserialization

---

## Root Cause
The CQL `CAST` function family registered a single `JavaFunctionWrapper` for every numeric conversion, including conversions from `counter` columns.
Counter column values are stored in a special binary encoding that differs from the `bigint` encoding even though both represent a 64-bit integer.
`JavaFunctionWrapper.execute` called `inputType().compose(bb)` to deserialise the raw bytes, but `CounterColumnType.compose` does not return the logical counter value — it returns an opaque `Long` whose bit representation reflects the on-disk counter shard format.
The fix introduced a `JavaCounterFunctionWrapper` subclass that overrides `compose()` to use `LongType.instance.compose(bb)` instead, which correctly interprets the wire-format bytes that arrive at function execution time as a plain 64-bit integer.

### Before / After
```java
// BEFORE — single wrapper used for all types, counter decompose wrong:
private static <O, I> void addFunctionIfNeeded(...) {
    if (!inputType.equals(outputType))
        functions.add(JavaFunctionWrapper.create(inputType, outputType, converter));
}

// BEFORE — execute used inputType.compose, wrong for counter:
return outputType().decompose(converter.apply(inputType().compose(bb)));

// AFTER — detect counter type and use dedicated wrapper:
private static <O, I> Function wrapJavaFunction(...) {
    return inputType.equals(CounterColumnType.instance)
            ? JavaCounterFunctionWrapper.create(outputType, (Function<Long, O>) converter)
            : JavaFunctionWrapper.create(inputType, outputType, converter);
}

// AFTER — JavaCounterFunctionWrapper overrides compose() to use LongType:
protected Long compose(ByteBuffer bb) {
    return LongType.instance.compose(bb);
}
```

---

## How to Spot
Counter columns are stored differently from regular `bigint` values at the CQL/serialization boundary. Any code that calls `type.compose(bb)` on a value whose column definition could be `counter` must either special-case `CounterColumnType` or confirm that the wire format reaching that point has already been converted to the standard `bigint` representation. When adding new functions or casts over numeric types, always include counter columns in the test matrix.

---

## Test
`CastFctsTest.testCounterCastsInSelectionClause` was added; it creates a counter table, increments the counter, and verifies that `CAST(b AS tinyint/smallint/int/bigint/...)` returns the correct numeric value.
