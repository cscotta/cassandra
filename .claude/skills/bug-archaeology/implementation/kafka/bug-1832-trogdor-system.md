**Commit**: 161d1cdf85 · **Ticket**: KAFKA-18218
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        with node.account.monitor_log(log_path) as monitor:

// After
        with node.account.monitor_log(stdout_stderr_capture_path) as monitor:
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18218 fix trogdor system test (#18156).

**Test**
Test modified in trogdor.py covering the corrected behaviour.
