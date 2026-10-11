**Commit**: 2966856049 · **Ticket**: KAFKA-18240
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>, TaiJuWu <tjwu1217@gmail.com>, Yung <yungyung7654321@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        node.account.create_file(get_log4j_config_for_tools(node).LOG4J_CONFIG, log_config)

// After
        node.account.create_file(get_log4j_config_for_tools(node), log_config)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18240 remove nonexistent `log4j_config` from transactional_message_copier.py (#18184).

**Test**
Test modified in transactional_message_copier.py covering the corrected behaviour.
