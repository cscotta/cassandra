**Commit**: 08ef22d888 · **Ticket**: KAFKA-18173
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        TestUtils.assertFutureError(topLevelErrorResult.all(), GroupAuthorizationException.class);
        TestUtils.assertFutureError(missingPartitionResult.all(), IllegalArgumentException.class);
        TestUtils.assertFutureError(missingPartitionResult.partitionResult(tpOne), IllegalArgumentException.class);
        TestUtils.assertFutureError(partitionLevelErrorResult.all(), UnknownTopicOrPartitionException.class);
        TestUtils.assertFutureError(partitionLevelErrorResult.partitionResult(tpOne), UnknownTopicOrPartitionException.class);
            TestUtils.assertFutureError(future, TimeoutException.class);
            TestUtils.assertFutureError(future, SaslAuthenticationException.class);
            TestUtils.assertFutureError(futur

// After
        TestUtils.assertFutureThrows(topLevelErrorResult.all(), GroupAuthorizationException.class);
        TestUtils.assertFutureThrows(missingPartitionResult.all(), IllegalArgumentException.class);
        TestUtils.assertFutureThrows(missingPartitionResult.partitionResult(tpOne), IllegalArgumentException.class);
        TestUtils.assertFutureThrows(partitionLevelErrorResult.all(), UnknownTopicOrPartitionException.class);
        TestUtils.assertFutureThrows(partitionLevelErrorResult.partitionResult(tpOne), UnknownTopicOrPartitionException.class);
import org.apache.kafka.common.message.AlterPartitionReassignmentsResponseData.ReassignablePartitionResponse;
import org.apache.kafka.common.message.AlterPartitionReassignmentsResponseData.ReassignableTopicResponse;
import org.apache.kafka.common.message.ListPartitionReassignmentsResponseData.OngoingPartitionReassignment;
import org.apache.kafka.common.message.ListPartitionReassignmentsResponseData.OngoingTopicReassignment;
            TestUtils.assertFutureThrows(future, TimeoutException.class);
            TestUtils.assertFutureThrows(future, SaslAuthenticationException.class);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18173 remove duplicate `assertfutureerror` (#18296).

**Test**
Test modified in DeleteConsumerGroupOffsetsResultTest.java covering the corrected behaviour.
