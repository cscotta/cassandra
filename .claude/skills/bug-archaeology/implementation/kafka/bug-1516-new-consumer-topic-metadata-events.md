**Commit**: 97a228070e · **Ticket**: KAFKA-18619
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
    verifyWithRetry(sendOneRecord(producer2))
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
  def testTransactionalProducerWithAuthenticationFailure(quorum: String, groupProtocol: String): Unit = {
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
    verifyWithRetry(sendOneRecord(producer))
    verifyWithRetry(assertEquals(1, consumer.poll(Duration.ofMillis(1000)).count))
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
  def testKafkaAdminClientWithAuthenticationFailure(quorum: String, groupProtocol: String): Unit = {

// After

    @Override
    public boolean requireSubscriptionMetadata() {
        return true;
    }
    verifyWithRetry(sendOneRecord(producer2))()
  @ParameterizedTest
  @ValueSource(strings = Array("kraft"))
  def testTransactionalProducerWithAuthenticationFailure(quorum: String): Unit = {
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18619: new consumer topic metadata events should set requiremetadata flag (#18668).

**Test**
Test modified in SaslClientsWithInvalidCredentialsTest.scala covering the corrected behaviour.
