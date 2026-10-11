# Bug 0603: Loss of Digits When Casting varint/bigint to decimal

**Commit**: 3fa7c08944 · **Ticket**: CASSANDRA-14170
**Subsystem**: `src/java/org/apache/cassandra/cql3/functions/CastFcts.java`
**Category**: Logic error in condition

---

## Root Cause

The CAST function for all numeric types to `decimal` used `BigDecimal.valueOf(p.doubleValue())` unconditionally. For integer types (`bigint`, `varint`) that approach first converts the value to a `double`, which has only 53 bits of mantissa; this silently truncates large integers such as `Long.MAX_VALUE` (9223372036854775807) to the nearest representable double. The fix introduces `getDecimalConversionFunction` that selects the lossless conversion path based on the input type: for `float`/`double` the `doubleValue()` path is preserved, for `varint` a `BigInteger`-based constructor is used, and for all other integer types `BigDecimal.valueOf(p.longValue())` is used, which does not pass through floating-point representation.

### Before / After

```java
// BEFORE — all integer types go through double, losing precision
addFunctionIfNeeded(functions, inputType, DecimalType.instance,
    p -> BigDecimal.valueOf(p.doubleValue()));
```

```java
// AFTER — type-appropriate conversion selected
private static <I extends Number> Function<I, BigDecimal>
    getDecimalConversionFunction(AbstractType<? extends Number> inputType)
{
    if (inputType == FloatType.instance || inputType == DoubleType.instance)
        return p -> BigDecimal.valueOf(p.doubleValue());
    if (inputType == IntegerType.instance)   // varint → BigInteger
        return p -> new BigDecimal((BigInteger) p);
    return p -> BigDecimal.valueOf(p.longValue());
}
```

---

## How to Spot

Whenever numeric conversions pass through floating-point as an intermediate step, check whether the source type can hold values that exceed the precision of `double` (53-bit mantissa). Any conversion chain that calls `.doubleValue()` on an integer type is a candidate for silent truncation of large values.

---

## Test

`testNoLossOfPrecisionForCastToDecimal` inserts `Long.MAX_VALUE` and a large `varint`, then asserts that `CAST(... AS decimal)` returns the exact `BigDecimal` value without rounding.
