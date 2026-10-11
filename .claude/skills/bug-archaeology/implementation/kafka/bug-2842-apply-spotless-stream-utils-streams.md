**Commit**: 09b4ef416a · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                               ':streams:streams-scala',
                               ':streams:test-utils',
                               ':streams:upgrade-system-tests-0100',
                               ':streams:upgrade-system-tests-0101',
                               ':streams:upgrade-system-tests-0102',
                               ':streams:upgrade-system-tests-0110',
                               ':streams:upgrade-system-tests-10',
                               ':streams:upgrade-system-tests-11',
                               ':streams:upgrade-system-tests-20',
                               ':streams:upgrade-system-tests-21',
                               ':streams:upgrade-system-tests-22',
                               ':streams:upgrade-system-tests-23',

// After
                               ':streams:streams-scala']
import org.apache.kafka.streams.TopologyConfig.TaskConfig;



```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to `stream:test-utils` and `:streams:upgrade-system-tests-xxxx`  (#16357).

**Test**
Test modified in TopologyTestDriver.java covering the corrected behaviour.
