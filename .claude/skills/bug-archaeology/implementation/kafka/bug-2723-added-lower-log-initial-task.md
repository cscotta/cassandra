**Commit**: 4cec840baf · **Ticket**: KAFKA-16661
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
added a lower log.initial.task.delay.ms value to integration test framework to 500ms

Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
}

// After
    props.put(ServerLogConfigs.LOG_INITIAL_TASK_DELAY_MS_CONFIG, "100")
}
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16661: added a lower `log.initial.task.delay.ms` value (#16221).

**Test**
Test modified in TestUtils.scala covering the corrected behaviour.
