**Commit**: 924c1081dc · **Ticket**: KAFKA-17415
**Subsystem**: metadata/src
**Category**: Resource leak

**Root cause**
Both ZK and KRaft modes do not handle overflow, so setting a large max lifetime results in a negative expired timestamp and negative max timestamp, which is unexpected behavior. In this PR, we are only fixing the KRaft code since ZK will be removed soon. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  val maxLifeTime = 5000
    this.serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_MAX_LIFETIME_CONFIG, maxLifeTime.toString)
  def testCreateDelegationTokenWithLargeTimeout(quorum: String): Unit = {
    val timeout = Long.MaxValue
    assertEquals(maxLifeTime, tokenInfo.maxTimestamp - tokenInfo.issueTimestamp)
    val timeout = -1
    assertEquals(maxLifeTime, tokenInfo.maxTimestamp - tokenInfo.issueTimestamp)
    val createDelegationTokenOptions = new CreateDelegationTokenOptions()

// After
    this.serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_EXPIRY_TIME_MS_CONFIG, Long.MaxValue.toString)
    this.serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_MAX_LIFETIME_CONFIG, Long.MaxValue.toString)
  def testCreateDelegationTokenWithSmallerTimeout(quorum: String): Unit = {
    val timeout = 5000
    assertEquals(timeout, tokenInfo.maxTimestamp - tokenInfo.issueTimestamp)
    val timeout = 5000
    assertEquals(timeout, tokenInfo.maxTimestamp - tokenInfo.issueTimestamp)
    val createDelegationTokenOptions = new CreateDelegationTokenOptions().maxlifeTimeMs(5000)
  @ParameterizedTest
  @ValueSource(strings = Array("kraft"))
  def testCreateTokenWithOverflowTimestamp(quorum: String): Unit = {
    client = createAdminClient
```

**How to spot**
When reviewing metadata/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17415 avoid overflow of expired timestamp (#17026).

**Test**
Test modified in SaslSslAdminIntegrationTest.scala covering the corrected behaviour.
