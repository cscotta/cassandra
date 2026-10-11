**Commit**: d001b47093 · **Ticket**: KAFKA-17792
**Subsystem**: connect/api
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>, Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import java.math.RoundingMode;
            BigDecimal ceil = decimal.setScale(0, RoundingMode.CEILING);
            BigDecimal floor = decimal.setScale(0, RoundingMode.FLOOR);
            if (ceil.equals(floor)) {
                BigInteger num = ceil.toBigIntegerExact();
                if (ceil.precision() >= 19 && (num.compareTo(LONG_MIN) < 0 || num.compareTo(LONG_MAX) > 0)) {

// After

        private static boolean isWholeNumber(BigDecimal bd) {
            return bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0;
        }

        private static final BigDecimal BIGGER_THAN_LONG = new BigDecimal("1e19");

            BigDecimal abs = decimal.abs();
            if (abs.compareTo(BIGGER_THAN_LONG) > 0 || (abs.compareTo(BigDecimal.ONE) < 0 && abs.compareTo(BigDecimal.ZERO) != 0)) {
                return null;
            }
            if (isWholeNumber(decimal)) {
```

**How to spot**
When reviewing connect/api code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17792: efficiently parse decimals with large exponents in connect values (#17510).

**Test**
Test modified in ValuesTest.java covering the corrected behaviour.
