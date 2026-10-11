# Bug 0540: Decimal Arithmetic Silently Truncates Results to 34 Significant Digits

**Commit**: d60e798873 · **Ticket**: CASSANDRA-15232
**Subsystem**: `src/java/org/apache/cassandra/db/marshal/DecimalType.java`
**Category**: Wrong constant / default value

---

## Root Cause

All five arithmetic operations on the CQL `decimal` type (`+`, `-`, `*`, `/`, `%`) used `MathContext.DECIMAL128`, which caps precision at 34 significant digits. For `decimal` columns—which use arbitrary-precision `BigDecimal` internally—this silently rounds or truncates values that require more than 34 digits. For division, `DECIMAL128` can also produce an `ArithmeticException` when the implicit division in a modulo operation generates a quotient wider than 34 digits. The fix raises the precision cap for add/subtract/multiply to 10,000 digits, computes a context-aware scale for division based on the operands' actual magnitudes, and removes the `MathContext` from modulo so that `BigDecimal.remainder` runs without a precision constraint.

### Before / After

```java
// BEFORE – all ops capped at DECIMAL128 (34 digits)
return decompose(left.add(right, MathContext.DECIMAL128));
return decompose(left.divide(right, MathContext.DECIMAL128));
return decompose(left.remainder(right, MathContext.DECIMAL128));
```

```java
// AFTER
private static final MathContext MAX_PRECISION = new MathContext(10000);
return decompose(left.add(right, MAX_PRECISION));
// divide: computes scale from operand magnitudes, min 32, max 1000
return decompose(left.divide(right, scale, RoundingMode.HALF_UP).stripTrailingZeros());
return decompose(left.remainder(right));  // no precision limit
```

---

## How to Spot

Whenever a type that advertises arbitrary precision (like `BigDecimal` or `BigInteger`) is operated on through a `MathContext` constant such as `DECIMAL128`, the precision guarantee is broken for inputs wider than that constant. Review every arithmetic call site on a "big" numeric type: if a `MathContext` is passed without a comment explaining why a bounded precision is acceptable, it is likely wrong for a column type that is supposed to hold arbitrary values.

---

## Test

Added `testDivisionWithDecimals` (very small / very large operands, and repeating-decimal quotient) and `testModuloWithDecimals` (large integer modulo) to assert exact `BigDecimal` results that would have been truncated or thrown before the fix.
