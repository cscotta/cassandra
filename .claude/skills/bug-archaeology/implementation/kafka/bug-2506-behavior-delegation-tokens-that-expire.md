**Commit**: 4a485ddb71 · **Ticket**: KAFKA-17315
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
In kraft mode, expiring delegation token (`expiryTimePeriodMs` < 0) has following different behavior to zk mode. 1. `ExpiryTimestampMs` is set to "expiryTimePeriodMs" [0] rather than "now" [1]
2. it throws exception directly if the token is expired already [2].

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.{ClusterAuthorizationException, InvalidRequestException, TopicAuthorizationException, UnknownTopicOrPartitionException}
import org.apache.kafka.server.config.{ServerConfigs, ZkConfigs}

// After
import org.apache.kafka.common.errors.{ClusterAuthorizationException, DelegationTokenExpiredException, DelegationTokenNotFoundException, InvalidRequestException, TopicAuthorizationException, UnknownTopicOrPartitionException}
import org.apache.kafka.common.security.token.delegation.DelegationToken
import org.apache.kafka.server.config.{DelegationTokenManagerConfigs, ServerConfigs, ZkConfigs}
    // Enable delegationTokenControlManager
    serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_CONFIG, "123")
    serverConfig.setProperty(DelegationTokenManagerConfigs.DELEGATION_TOKEN_MAX_LIFETIME_CONFIG, "5000")

  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft"))
  def testExpireDelegationToken(quorum: String): Unit = {
    client = createAdminClient
    val createDelegationTokenOptions = new CreateDelegationTokenOptions()
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17315 fix the behavior of delegation tokens that expire immediately upon creation in kraft mode (#16858).

**Test**
Test modified in SaslSslAdminIntegrationTest.scala covering the corrected behaviour.
