**Commit**: c32693d556 · **Ticket**: KAFKA-15623
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertTrue;
@RunWith(MockitoJUnitRunner.StrictStubs.class)
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertThrows;
        Assert.assertThrows(
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Before;

// After
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertTrue;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
        assertThrows(
import static org.junit.jupiter.api.Assertions.assertThrows;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15623 (1/n) migrate streams tests module to junit 5 (#16254).

**Test**
Test modified in AbstractStreamTest.java covering the corrected behaviour.
