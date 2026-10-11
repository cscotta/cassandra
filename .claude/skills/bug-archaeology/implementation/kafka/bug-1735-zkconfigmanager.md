**Commit**: dbf4602c13 · **Ticket**: KAFKA-18367
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{ConfigType, ZkConfigs}
  var dynamicConfigHandlers: Map[String, ConfigHandler] = _
  private var dynamicConfigManager: ZkConfigManager = _
        /* start dynamic config manager */
        dynamicConfigHandlers = Map[String, ConfigHandler](ConfigType.TOPIC -> new TopicConfigHandler(replicaManager, config, quotaManagers, Some(kafkaController)),
                                                           ConfigType.CLIENT -> new ClientIdConfigHandler(quotaManagers),
                                                           ConfigType.USER -> new UserConfigHandler(quotaManagers, credentialProvider),
                                                           ConfigType.BROKER -> new BrokerConfigHandler(config, quotaManagers),
                                                           ConfigType.IP -> new IpConfigHandler(socketServer.connectionQuotas))

        // Create the config manager. start listening to notifications
        dynamicConfigManager = new ZkConfigManager(zkClient, dynamicConfigHandlers)

// After
import org.apache.kafka.server.config.ZkConfigs
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18367 remove zkconfigmanager (#18363).

**Test**
No test added.
