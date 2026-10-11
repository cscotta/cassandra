**Commit**: 0a203a9622 · **Ticket**: KAFKA-16938
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  private val nonDynamicProps: Set[String] = KafkaConfig.configNames.toSet -- DynamicConfig.Broker.names.asScala

    props.asScala.keySet.intersect(nonDynamicProps)
  private[server] def addDynamicConfigs(configDef: ConfigDef): Unit = {
    KafkaConfig.configKeys.forKeyValue { (configName, config) =>
      if (AllDynamicConfigs.contains(configName)) {
        configDef.define(config.name, config.`type`, config.defaultValue, config.validator,
          config.importance, config.documentation, config.group, config.orderInGroup, config.width,
          config.displayName, config.dependents, config.recommender)
      }
    }
  }

// After
    props.asScala.keySet.intersect(DynamicConfig.Broker.nonDynamicProps)
    val nonDynamic = configNames.intersect(DynamicConfig.Broker.nonDynamicProps)
import kafka.server.DynamicBrokerConfig.AllDynamicConfigs

    object Broker {
      private val brokerConfigs = {
        val configs = QuotaConfigs.brokerQuotaConfigs()

        // Filter and define all dynamic configurations
        KafkaConfig.configKeys
          .filter { case (configName, _) => AllDynamicConfigs.contains(configName) }
          .foreach { case (_, config) => configs.define(config) }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16938 non-dynamic props gets corrupted due to circular reference between dynamicbrokerconfig and dynamicconfig. (#.

**Test**
No test added.
