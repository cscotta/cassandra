**Commit**: 46597638ee · **Ticket**: KAFKA-17371
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import static org.junit.jupiter.api.Assertions.assertNotNull;
        assertNotNull(taskExecutor.currentTask());

// After
import org.apache.kafka.test.TestUtils;
        TestUtils.waitForCondition(() -> taskExecutor.currentTask() != null,
                VERIFICATION_TIMEOUT,
                "Task reassign take too much time");
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17371: fix defaulttaskexecutortest.shouldunassigntaskwhenrequired (#16941).

**Test**
Test modified in DefaultTaskExecutorTest.java covering the corrected behaviour.
