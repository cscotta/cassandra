**Commit**: 823d6f7555 · **Ticket**: KAFKA-16958
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16958 add strict_stubs to endtoendlatencytest, offsetcommitcallbackinvokertest, producerperformancetest, and topol.

**Test**
Test modified in OffsetCommitCallbackInvokerTest.java covering the corrected behaviour.
