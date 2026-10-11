**Commit**: 808498e939 · **Ticket**: KAFKA-14401
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before

            try {
                log.trace("{} started execution", this);
                while (true) {
                    int numCallbacks;
                                     "This may occur when brokers are unavailable or unreachable. Reason: {}", topic, e.getMessage());
                                     "Reason: {}", topic, e.getMessage());
            } catch (Throwable t) {
                log.error("Unexpected exception in {}", this, t);

// After
            log.trace("{} started execution", this);
            while (true) {
                int numCallbacks = 0;
                try {
                                "This may occur when brokers are unavailable or unreachable. Reason: {}", topic, e.getMessage());
                                "Reason: {}", topic, e.getMessage());
                } catch (Throwable t) {
                    log.error("Unexpected exception in {}", this, t);
                    synchronized (KafkaBasedLog.this) {
                        // Only fail exactly the number of callbacks we found before triggering the read to log end
                        // since it is possible for another write + readToEnd to sneak in the meantime which we don't
                        // want to fail.
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14401: fail kafka log read end requests if underneath work thread fails (#14372).

**Test**
Test modified in KafkaBasedLogTest.java covering the corrected behaviour.
