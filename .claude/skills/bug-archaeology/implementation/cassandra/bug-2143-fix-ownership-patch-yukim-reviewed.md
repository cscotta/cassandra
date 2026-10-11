# Bug 2143: Murmur3Partitioner Ownership Calculation Overflows with float Arithmetic

**Commit**: e7ae7c9040 · **Ticket**: CASSANDRA-5076
**Subsystem**: src/java/org/apache/cassandra/dht/Murmur3Partitioner.java
**Category**: Off-by-one

---

## Root Cause

`Murmur3Partitioner.describeOwnership()` computed token range ownership percentages using `float` arithmetic on `long` token values. The Murmur3 token space spans from `Long.MIN_VALUE` to `Long.MAX_VALUE`, a range of approximately 2^64. Subtracting two `long` values and adding the range constant `MAXIMUM` (cast to `float`) caused precision loss because `float` has only 24 bits of mantissa — far too few to represent 64-bit differences accurately. The modulo and division then produced incorrect percentages that did not sum to 1.0. The fix converts all arithmetic to `BigInteger`/`BigDecimal` to preserve full precision across the entire token range.

### Before / After

```java
// BEFORE: float arithmetic on long tokens causes precision loss
final float ri = MAXIMUM;
Token start = i.next(); Long ti = ((LongToken)start).token;
Token t; Long tim1 = ti;
while (i.hasNext()) {
    t = i.next(); ti = ((LongToken) t).token;
    float age = ((ti - tim1 + ri) % ri) / ri;  // float overflow/precision loss
    ...
}
```

```java
// AFTER: BigInteger/BigDecimal arithmetic preserves full precision
final BigInteger ri = BigInteger.valueOf(MAXIMUM).subtract(BigInteger.valueOf(MINIMUM.token + 1));
final BigDecimal r  = new BigDecimal(ri);
Token start = i.next(); BigInteger ti = BigInteger.valueOf(((LongToken)start).token);
Token t; BigInteger tim1 = ti;
while (i.hasNext()) {
    t = i.next(); ti = BigInteger.valueOf(((LongToken) t).token);
    float age = new BigDecimal(ti.subtract(tim1).add(ri).mod(ri))
                    .divide(r, 6, BigDecimal.ROUND_HALF_EVEN).floatValue();
    ...
}
```

---

## How to Spot

Token-space arithmetic on `Murmur3Partitioner` (64-bit signed long space) must never use `float` or even `double` for intermediate calculations — the token range exceeds what either type can represent without rounding. Review any ownership, split, or midpoint calculation in hash-based partitioners: use `BigInteger` for differences and `BigDecimal` for divisions.

---

## Test

`PartitionerTestCase.testDescribeOwnership()` added, verifying that the sum of all token ownership percentages equals 1.0 (within 0.001 tolerance) for 1, 2, and 256 tokens.
