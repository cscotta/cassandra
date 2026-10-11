**Commit**: e5598a9d8c · **Ticket**: KAFKA-18040
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Test lacked the proper configuration for the offset topic replication. As a result, when the follower was shut down, the coordinator did not failover properly. Reviewers: TaiJuWu <tjwu1217@gmail.com>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly_KAFKA_16176"))
  def getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly_KAFKA_16176: stream.Stream[Arguments] = getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly

// After
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig
    overridingProps.put(GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG, 2.toShort)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18040; fix for test that ensures produce during follower shutdown (#18108).

**Test**
Test modified in BaseProducerSendTest.scala covering the corrected behaviour.
