**Commit**: fde6ae1500 · **Ticket**: KAFKA-18029
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        :param dynamicRaftQuorum: When true, the quorum uses kraft.version=1, controller_quorum_bootstrap_servers, and bootstraps the first controller using the standalone flag
                        cmd += " --feature kraft.version=1"

// After
        :param dynamicRaftQuorum: When true, controller_quorum_bootstrap_servers, and bootstraps the first controller using the standalone flag
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18029 remove the `kraft.version=1` from kafka.py (#17838).

**Test**
Test modified in kafka.py covering the corrected behaviour.
