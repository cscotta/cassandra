**Commit**: b2b2408692 · **Ticket**: KAFKA-18360
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Remove broker.id.generation.enable and reserved.broker.max.id, which are not used in KRaft mode. Remove inter.broker.protocol.version, which is not used in KRaft mode. Reviewers: PoAn Yang <payang@apache.org>, Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  // We keep the user-provided String as `MetadataVersion.fromVersionString` can choose a slightly different version (eg if `0.10.0`
  // is passed, `0.10.0-IV0` may be picked)
  val interBrokerProtocolVersionString = getString(ReplicationConfigs.INTER_BROKER_PROTOCOL_VERSION_CONFIG)
  val interBrokerProtocolVersion = if (processRoles.isEmpty) {
    MetadataVersion.fromVersionString(interBrokerProtocolVersionString)
  } else {
    if (originals.containsKey(ReplicationConfigs.INTER_BROKER_PROTOCOL_VERSION_CONFIG)) {
      // A user-supplied IBP was given
      val configuredVersion = MetadataVersion.fromVersionString(interBrokerProtocolVersionString)
      if (!configuredVersion.isKRaftSupported) {
        throw new ConfigException(s"A non-KRaft version $interBrokerProtocolVersionString given for ${ReplicationConfigs.INTER_BROKER_PROTOCOL_VERSION_CONFIG}. " +
          s"The minimum version is ${MetadataVersion.MINIMUM_KRAFT_VERSION}")

// After
  // This will be removed soon. See KAFKA-18366.
  val interBrokerProtocolVersion = MetadataVersion.MINIMUM_KRAFT_VERSION
    if (processRoles.contains(ProcessRole.BrokerRole)) {
    val bootstrapDirectory = new BootstrapDirectory(config.metadataLogDir, Optional.empty())
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18360 remove zookeeper configurations (#18566).

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
