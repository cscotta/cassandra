**Commit**: 6eb6a5e578 · **Ticket**: KAFKA-18776
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                log.debug("Consumer has been disconnected from the group coordinator for {}ms", durationOfOngoingDisconnectMs);
            // You'd be forgiven for assuming that a warning message would be logged at WARN, but
            // markCoordinatorUnknown logs the warning at DEBUG. This is partly for historical parity with the
            // ClassicKafkaConsumer.
            appender.setClassLogger(CoordinatorRequestManager.class, Level.DEBUG);
        Pattern pattern = Pattern.compile("\\s+(?<millis>\\d+)+ms");

// After
                log.warn("Consumer has been disconnected from the group coordinator for {}ms", durationOfOngoingDisconnectMs);
            appender.setClassLogger(CoordinatorRequestManager.class, Level.WARN);
        Pattern pattern = Pattern.compile("^Consumer has been disconnected from the group coordinator for (?<millis>\\d+)+ms$");
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18776: fix flaky coordinator disconnect test & fix log level (#18866).

**Test**
Test modified in CoordinatorRequestManagerTest.java covering the corrected behaviour.
