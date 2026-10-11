**Commit**: 253b36113d · **Ticket**: KAFKA-17179
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.Tag;
@Tag("integration")
import org.junit.jupiter.api.Tag;
@Tag("integration")
import org.junit.jupiter.api.{Tag, Timeout}
@Tag("integration")
import org.junit.jupiter.api.{Tag, Timeout}
@Tag("integration")
import org.junit.jupiter.api.{Tag, Timeout}
@Tag("integration")

// After
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.Timeout
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17179 remove integration tag in class level when using clustertestextensions (#16656).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
