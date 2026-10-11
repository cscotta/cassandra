**Commit**: a1703e2cca · **Ticket**: KAFKA-17040
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
import static org.junit.jupiter.api.Assertions.assertThrows;
        assertThrows(IllegalStateException.class, () -> telemetrySender.timeToNextUpdate(100));

// After
                case TERMINATED:
                    timeMs = Long.MAX_VALUE;
                    log.trace("For telemetry state {}, returning the value {} ms; telemetry is terminated, no further requests will be made", localState, timeMs);
                    break;
        assertEquals(Long.MAX_VALUE, telemetrySender.timeToNextUpdate(100));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17040: removing exception on further calls to terminated telemetry reporter (#18143).

**Test**
Test modified in ClientTelemetryReporterTest.java covering the corrected behaviour.
