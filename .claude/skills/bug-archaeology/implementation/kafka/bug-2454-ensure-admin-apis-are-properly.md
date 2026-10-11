**Commit**: 359ddce3b2 · **Ticket**: KAFKA-17137
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    this.serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_CONFIG, secretKey)
    serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_CONFIG, "123")
    serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_MAX_LIFETIME_CONFIG, "5000")

// After
  val maxLifeTime = 5000
    this.serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_CONFIG, secretKey)
    this.serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_MAX_LIFETIME_CONFIG, maxLifeTime.toString)
  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft"))
  def testCreateDelegationTokenWithLargeTimeout(quorum: String): Unit = {
    client = createAdminClient
    val timeout = Long.MaxValue

    val options = new CreateDelegationTokenOptions().maxlifeTimeMs(timeout)
    val tokenInfo = client.createDelegationToken(options).delegationToken.get.tokenInfo

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17137 ensure admin apis are properly tested (token-related) (#16905).

**Test**
Test modified in SaslSslAdminIntegrationTest.scala covering the corrected behaviour.
