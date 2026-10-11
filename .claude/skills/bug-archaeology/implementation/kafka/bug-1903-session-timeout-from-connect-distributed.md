**Commit**: 6ef8b1c4c0 · **Ticket**: KAFKA-18132
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

# Reduce session timeouts so tests that kill workers don't need to wait as long to recover
session.timeout.ms=10000
consumer.session.timeout.ms=10000


// After
// (no additions)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18132 remove "session.timeout.ms" from connect-distributed.properties to fix connect e2e (#18005).

**Test**
Test modified in connect-distributed.properties covering the corrected behaviour.
