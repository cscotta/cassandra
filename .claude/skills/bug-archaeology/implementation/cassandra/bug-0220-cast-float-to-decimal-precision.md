# Bug 0220: CAST float to decimal Uses Double Conversion, Losing Precision

**Commit**: 7ec5126870 · **Ticket**: CASSANDRA-18647
**Subsystem**: cql3/functions (CastFcts)
**Category**: Logic error in condition

---

## Root Cause

`CastFcts.getDecimalConversionFunction()` used a single branch to handle both `FloatType` and `DoubleType`, converting via `BigDecimal.valueOf(p.doubleValue())`. For a `float` input, this first widened the 32-bit value to a 64-bit `double`, introducing floating-point representation artefacts, and then converted that imprecise `double` to `BigDecimal`. The correct path for `float` is `new BigDecimal(Float.toString(p.floatValue()))`, which converts through the canonical decimal string representation and preserves the value as a human would expect (e.g., `5.2f` → `"5.2"` → `BigDecimal("5.2")`). The fix splits the combined branch into two separate branches, one for each type.

### Before / After

```java
// BEFORE – float and double both converted via doubleValue(), losing float precision
if (inputType == FloatType.instance || inputType == DoubleType.instance)
    return p -> BigDecimal.valueOf(p.doubleValue());
```

```java
// AFTER – float converted via its string representation
if (inputType == FloatType.instance)
    return p -> new BigDecimal(Float.toString(p.floatValue()));

if (inputType == DoubleType.instance)
    return p -> BigDecimal.valueOf(p.doubleValue());
```

---

## How to Spot

Widening a `float` to `double` before constructing a `BigDecimal` is a well-known precision pitfall: `5.2f` becomes `5.199999809265137` as a `double`. When reviewing numeric type-conversion functions, ensure that `float`-to-decimal paths go through `Float.toString()` or `new BigDecimal(float)` rather than via an intermediate `double`. The symptom is that CAST(floatCol AS decimal) returns values with spurious trailing digits.

---

## Test

`CastFctsTest` was updated to assert that `CAST(5.2 AS decimal)` returns `new BigDecimal("5.2")` rather than `BigDecimal.valueOf(5.2F)` which produces `5.199999809265137`.
