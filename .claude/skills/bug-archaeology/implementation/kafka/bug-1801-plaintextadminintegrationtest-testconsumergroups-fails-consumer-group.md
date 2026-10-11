**Commit**: dfc07e0e0c · **Ticket**: KAFKA-17960
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The issue has been fixed by https://issues.apache.org/jira/browse/KAFKA-18188. We can re-enable the test with the CONSUMER protocol. Reviewers: Lianet Magrans <lmagrans@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly_KAFKA_17960"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly_KAFKA_17960"))
  def getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly_KAFKA_17960: stream.Stream[Arguments] = getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly
  def getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly_KAFKA_17961: stream.Stream[Arguments] = getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly
  def getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly_KAFKA_17964: stream.Stream[Arguments] = getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly

// After
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17960; plaintextadminintegrationtest.testconsumergroups fails with consumer group protocol (#18234).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
