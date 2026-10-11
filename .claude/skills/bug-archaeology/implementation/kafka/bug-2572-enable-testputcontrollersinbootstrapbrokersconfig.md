**Commit**: fac8cae388 · **Ticket**: KAFKA-17303
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.Disabled;
    @Disabled
            assertEquals(MismatchedEndpointTypeException.class, exception.getCause().getClass());
            assertEquals("This endpoint does not appear to be a BROKER.",
                    exception.getCause().getMessage());

// After
import org.apache.kafka.common.errors.UnsupportedVersionException;
            assertEquals(UnsupportedVersionException.class, exception.getCause().getClass());
            assertEquals("The node does not support METADATA", exception.getCause().getMessage());
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17303 enable testputcontrollersinbootstrapbrokersconfig (#16838).

**Test**
Test modified in BootstrapControllersIntegrationTest.java covering the corrected behaviour.
