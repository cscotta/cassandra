**Commit**: 8afa5e74ac · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
The new default standby task assignment in TaskAssignment should only assign standby tasks for changelogged tasks, not all stateful tasks. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
            .filter(TaskInfo::isStateful)
            .filter(TaskInfo::isStateful)

// After
            .filter(taskInfo -> taskInfo.topicPartitions().stream().anyMatch(TaskTopicPartition::isChangelog))
            .filter(taskInfo -> taskInfo.topicPartitions().stream().anyMatch(TaskTopicPartition::isChangelog))
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 26) default standby task assignment nit (#16331).

**Test**
No test added.
