**Commit**: b3939f7901 · **Ticket**: KAFKA-17832
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    @EnabledForJreRange(min = JRE.JAVA_8, max = JRE.JAVA_22)
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
    @EnabledForJreRange(min = JRE.JAVA_11)
    @EnabledForJreRange(min = JRE.JAVA_11)
    @EnabledForJreRange(min = JRE.JAVA_11)
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
@EnabledForJreRange(min = JRE.JAVA_11) // TLS 1.3 is only supported with Java 11 and newer

// After
    @EnabledForJreRange(max = JRE.JAVA_22)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17832 remove all enabledforjrerange (#17543).

**Test**
Test modified in SecurityManagerCompatibilityTest.java covering the corrected behaviour.
