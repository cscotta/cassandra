**Commit**: 023f9c26e6 · **Ticket**: KAFKA-18529
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kirk True <ktrue@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
import kafka.server.QuorumTestHarness.getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly
    assertEquals(1, getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly.count())
    val args = getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly
      .findFirst().get.get
    stream.Stream.of(
      Arguments.of((args :+ true):_*),
      Arguments.of((args :+ false):_*)
    )
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  def testDescribeUserScramCredentialsTimeout(quorum: String, groupProtocol: String): Unit = {

// After
import kafka.server.QuorumTestHarness.getTestQuorumAndGroupProtocolParametersAll
    getTestQuorumAndGroupProtocolParametersAll
      .flatMap { baseArgs =>
        stream.Stream.of(
          Arguments.of((baseArgs.get :+ true):_*),
          Arguments.of((baseArgs.get :+ false):_*)
        )
      }
  @ParameterizedTest
  @ValueSource(strings = Array("kraft"))
  def testDescribeUserScramCredentialsTimeout(quorum: String): Unit = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18529: consumerrebootstraptest should run for async consumer (#18554).

**Test**
Test modified in ConsumerRebootstrapTest.scala covering the corrected behaviour.
