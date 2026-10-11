**Commit**: a41f7a4e13 · **Ticket**: KAFKA-16884
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <gharris1727@gmail.com>, Kamal Chandraprakash <kamal.chandraprakash@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  RemoteLogManagerConfig.CONFIG_DEF.configKeys().values().forEach(key => configDef.define(key))
  private val _remoteLogManagerConfig = new RemoteLogManagerConfig(this)
import org.apache.kafka.common.config.AbstractConfig;
        AbstractConfig config = new AbstractConfig(RemoteLogManagerConfig.CONFIG_DEF, props);
        return new RemoteLogManagerConfig(config);
import org.apache.kafka.common.config.{AbstractConfig, TopicConfig}
    val config = new AbstractConfig(RemoteLogManagerConfig.CONFIG_DEF, props)
    val remoteLogManagerConfig = new RemoteLogManagerConfig(config)
    val config = new AbstractConfig(RemoteLogManagerConfig.CONFIG_DEF, props)
    val remoteLogManagerConfig = new RemoteLogManagerConfig(config)

// After
  RemoteLogManagerConfig.configDef().configKeys().values().forEach(key => configDef.define(key))
  private val _remoteLogManagerConfig = new RemoteLogManagerConfig(props)
        return new RemoteLogManagerConfig(props);
import org.apache.kafka.common.config.{TopicConfig}
    val remoteLogManagerConfig = new RemoteLogManagerConfig(props)
    val remoteLogManagerConfig = new RemoteLogManagerConfig(props)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16884 refactor remotelogmanagerconfig with abstractconfig (#16199).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
