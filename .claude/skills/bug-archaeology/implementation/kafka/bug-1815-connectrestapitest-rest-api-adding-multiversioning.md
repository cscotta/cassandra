**Commit**: a2a098de6a · **Ticket**: KAFKA-18246
**Subsystem**: tests/kafkatest
**Category**: Incorrect filtering / wrong query result

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                           'tasks.max.enforce'}
                         'tasks.max.enforce'}

// After
                           'tasks.max.enforce', 'connector.plugin.version', 'key.converter.plugin.version', 'value.converter.plugin.version',
                           'header.converter.plugin.version'}
                         'tasks.max.enforce', 'connector.plugin.version', 'key.converter.plugin.version', 'value.converter.plugin.version',
                           'header.converter.plugin.version'}
```

**How to spot**
When reviewing tests/kafkatest code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18246 fix connectrestapitest.test_rest_api by adding multiversioning configs (#18191).

**Test**
Test modified in connect_rest_test.py covering the corrected behaviour.
