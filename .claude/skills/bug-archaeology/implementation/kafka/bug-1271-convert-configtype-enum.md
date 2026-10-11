**Commit**: 20bad6efb3 · **Ticket**: KAFKA-18576
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
JIRA: KAFKA-18576
After removing ZooKeeper, we no longer need to exclude `client_metrics`
and `group` from `ConfigType#ALL`. Since it's a common pattern to provide a mechanism to know all values in
enumeration ( Java enum provides ootb), we should convert ConfigType to
enum. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  private val BrokerSupportedConfigTypes = ConfigType.ALL.asScala :+ BrokerLoggerConfigType
      case ConfigType.TOPIC | ConfigType.CLIENT_METRICS | ConfigType.BROKER | ConfigType.GROUP =>
          case ConfigType.TOPIC => ConfigResource.Type.TOPIC
          case ConfigType.CLIENT_METRICS => ConfigResource.Type.CLIENT_METRICS
          case ConfigType.BROKER => ConfigResource.Type.BROKER
          case ConfigType.GROUP => ConfigResource.Type.GROUP
      case ConfigType.USER | ConfigType.CLIENT =>
        if (entityTypeHead == ConfigType.CLIENT || entityTypes.size == 2) { // size==2 for case where users is specified first on the command line, before clients
            throw new IllegalArgumentException(s"Only quota configs can be added for '${ConfigType.CLIENT}' using --bootstrap-server. Unexpected config names: ${unknownConfigsToAdd ++ scramConfigsToAddMap.keys}")
            throw new IllegalArgumentException(s"Only quota configs can be deleted for '${ConfigType.CLIENT}' using --bootstrap-server. Unexpected config names: ${unknownConfigsToDelete ++ scramConfigsToDelete}")
            throw new IllegalArgumentException(s"Only quota and SCRAM credential configs can be added for '${ConfigType.USER}' using --bootstrap-server. Unexpected config names: $unknownConfigsToAdd")
            throw new IllegalArgumentException(s"Only quota and SCRAM credential configs can be deleted for '${ConfigType.USER}' using --bootstrap-server. Unexpected config names: $unknownConfigsToDelete")

// After
  private val BrokerSupportedConfigTypes = ConfigType.values.map(_.value) :+ BrokerLoggerConfigType
  private val TopicType = ConfigType.TOPIC.value
  private val ClientMetricsType = ConfigType.CLIENT_METRICS.value
  private val BrokerType = ConfigType.BROKER.value
  private val GroupType = ConfigType.GROUP.value
  private val UserType = ConfigType.USER.value
  private val ClientType = ConfigType.CLIENT.value
  private val IpType = ConfigType.IP.value
      case TopicType | ClientMetricsType | BrokerType | GroupType =>
          case TopicType => ConfigResource.Type.TOPIC
          case ClientMetricsType => ConfigResource.Type.CLIENT_METRICS
          case BrokerType => ConfigResource.Type.BROKER
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18576 convert configtype to enum (#18711).

**Test**
Test modified in ConfigCommandTest.java covering the corrected behaviour.
