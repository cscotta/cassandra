**Commit**: 4a5ae144ea · **Ticket**: KAFKA-19032
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The zookeeper mode was removed in 4.0. The test cases don't need to
specify quorum. Following variable and functions can be replaced:
- TestWithParameterizedQuorumAndGroupProtocolNames
- getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly
- getTestQuorumAndGroupProtocolParametersConsumerGroupProtocolOnly
- getTestQuorumAndGroupProtocolParametersAll

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  @CsvSource(value = Array("kraft,false", "kraft,true"))
  def testTopicIdAuthorization(quorum: String, withTopicExisting: Boolean): Unit = {
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  def testConsumeUsingAssignWithNoAccess(quorum: String, groupProtocol: String): Unit = {
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  def testSimpleConsumeWithOffsetLookupAndNoGroupAccess(quorum: String, groupProtocol: String): Unit = {
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  def testSimpleConsumeWithExplicitSeekAndNoGroupAccess(quorum: String, groupProtocol: String): Unit = {
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)

// After
  @CsvSource(value = Array("false", "true"))
  def testTopicIdAuthorization(withTopicExisting: Boolean): Unit = {
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedGroupProtocolNames)
  @MethodSource(Array("getTestGroupProtocolParametersAll"))
  def testConsumeUsingAssignWithNoAccess(groupProtocol: String): Unit = {
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedGroupProtocolNames)
  @MethodSource(Array("getTestGroupProtocolParametersAll"))
  def testSimpleConsumeWithOffsetLookupAndNoGroupAccess(groupProtocol: String): Unit = {
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedGroupProtocolNames)
  @MethodSource(Array("getTestGroupProtocolParametersAll"))
  def testSimpleConsumeWithExplicitSeekAndNoGroupAccess(groupProtocol: String): Unit = {
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedGroupProtocolNames)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19032 remove testinfoutils.testwithparameterizedquorumandgroupprotocolnames (#19270).

**Test**
Test modified in AuthorizerIntegrationTest.scala covering the corrected behaviour.
