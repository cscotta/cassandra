**Commit**: 6d9ef0e12a · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                               ':group-coordinator:group-coordinator-api', // https://github.com/apache/kafka/pull/16198
                               ':group-coordinator',
import org.apache.kafka.coordinator.group.classic.ClassicGroup;
import org.apache.kafka.common.requests.DescribeGroupsRequest;
import org.apache.kafka.common.requests.DeleteGroupsRequest;
import org.apache.kafka.coordinator.group.runtime.CoordinatorShardBuilderSupplier;
import org.apache.kafka.common.message.ConsumerGroupDescribeResponseData;
import org.apache.kafka.common.message.DescribeGro

// After
        targetExclude 'src/generated/**/*.java'
import org.apache.kafka.coordinator.group.classic.ClassicGroup;
import org.apache.kafka.common.requests.DeleteGroupsRequest;
import org.apache.kafka.common.requests.DescribeGroupsRequest;
import org.apache.kafka.coordinator.group.runtime.CoordinatorShardBuilderSupplier;

import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.message.ConsumerGroupDescribeResponseData;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to `group-coordinator` and `group-coordinator-api`  (#16298).

**Test**
Test modified in Assertions.java covering the corrected behaviour.
