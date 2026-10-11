**Commit**: 9cc1547672 · **Ticket**: KAFKA-18247
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After

    Logger:
      - name: org.apache.kafka.clients.consumer.ConsumerConfig
        level: ERROR
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18247 use log4j2 yaml to rewrite the fix of kafka-18145 (#18192).

**Test**
Test modified in connect_log4j2.yaml covering the corrected behaviour.
