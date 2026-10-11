**Commit**: 6737178c12 · **Ticket**: KAFKA-18280
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        else:
            self.minikdc = None
            if self.quorum_info.using_kraft:
                self.controller_quorum.minikdc = None
                if self.isolated_kafka:
                    self.isolated_kafka.minikdc = None

// After
// (no additions)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18280 fix e2e testsecurityrollingupgrade.test_rolling_upgrade_sasl_mechanism_phase_one (#18308).

**Test**
Test modified in kafka.py covering the corrected behaviour.
