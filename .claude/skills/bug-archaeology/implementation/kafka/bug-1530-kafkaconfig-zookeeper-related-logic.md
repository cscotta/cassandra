**Commit**: c40e7a1341 · **Ticket**: KAFKA-18533
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{AbstractKafkaConfig, DelegationTokenManagerConfigs, KRaftConfigs, QuotaConfig, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ZkConfigs}
  /** ********* Zookeeper Configuration ***********/
  val zkConnect: String = getString(ZkConfigs.ZK_CONNECT_CONFIG)
  val zkSessionTimeoutMs: Int = getInt(ZkConfigs.ZK_SESSION_TIMEOUT_MS_CONFIG)
  val zkConnectionTimeoutMs: Int =
    Option(getInt(ZkConfigs.ZK_CONNECTION_TIMEOUT_MS_CONFIG)).map(_.toInt).getOrElse(getInt(ZkConfigs.ZK_SESSION_TIMEOUT_MS_CONFIG))
  val zkEnableSecureAcls: Boolean = getBoolean(ZkConfigs.ZK_ENABLE_SECURE_ACLS_CONFIG)
  val zkMaxInFlightRequests: Int = getInt(ZkConfigs.ZK_MAX_IN_FLIGHT_REQUESTS_CONFIG)

  def requiresZookeeper: Boolean = processRoles.isEmpty
  def usesSelfManagedQuorum: Boolean = processRoles.nonEmpty


// After
import org.apache.kafka.server.config.{AbstractKafkaConfig, DelegationTokenManagerConfigs, KRaftConfigs, QuotaConfig, ReplicationConfigs, ServerConfigs, ServerLogConfigs}
    if (!originals.containsKey(SocketServerConfigs.LISTENER_SECURITY_PROTOCOL_MAP_CONFIG)) {
      // validations for all broker setups (i.e. broker-only and co-located)
    contextLabels.put(NodeIdLabel, config.nodeId.toString)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18533 remove kafkaconfig zookeeper related logic (#18547).

**Test**
Test modified in DescribeAuthorizedOperationsTest.scala covering the corrected behaviour.
