**Commit**: 0e346d3103 · **Ticket**: KAFKA-15623
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  zstd: "1.5.6-3"
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
    @Before
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertThrows;
@RunWith(MockitoJUnitRunner.StrictStubs.class)
import org.junit.Test;

// After
    testImplementation libs.junitPlatformSuiteEngine // supports suite test
  zstd: "1.5.6-3",
  junitPlatformSuiteEngine: "1.10.2"
  junitPlatformSuiteEngine: "org.junit.platform:junit-platform-suite-engine:$versions.junitPlatformSuiteEngine",
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
    @BeforeEach
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15623 (4/n) migrate streams tests (processor) module to junit 5 (#16396).

**Test**
Test modified in AbstractProcessorContextTest.java covering the corrected behaviour.
