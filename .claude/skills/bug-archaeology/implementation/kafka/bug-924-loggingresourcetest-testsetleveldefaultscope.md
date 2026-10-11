**Commit**: dc82c766fa · **Ticket**: KAFKA-18834
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
- Flaky behavior
`LoggingResourceTest#testSetLevelDefaultScope` sometimes fails by not
capturing its expected WARN log. - Root cause
Both `LoggersTest#testSetLevelWithValidRootLoggerNames` and
`LoggingResourceTest#testSetLevelDefaultScope` may share the same
`LoggerContext` when executed in the same JVM. `LoggersTest#testSetLevelWithValidRootLoggerNames` calls
`loggers.setLevel("", ERROR)`, which mutates the global root logger
level to ERROR and suppresses WARN logs, which causes subsequent tests
to fail to emit WARN-level output. - Fix in this PR
Resets the Log4j configuration after each test in `LoggersTest`,
ensuring that any global changes are reverted.

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.AfterEach;

    @AfterEach
    public void tearDown() {
        // Reset LoggerContext to its initial configuration.
        // This ensures any log level changes made in a test do not leak into subsequent tests.
        LoggerContext.getContext(false).reconfigure();
    }
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18834 fix loggingresourcetest#testsetleveldefaultscope (#19920).

**Test**
Test modified in LoggersTest.java covering the corrected behaviour.
