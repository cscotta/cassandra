**Commit**: cd48fa682d · **Ticket**: KAFKA-17077
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  var brokerId: Int = getInt(ServerConfigs.BROKER_ID_CONFIG)
  val nodeId: Int = getInt(KRaftConfigs.NODE_ID_CONFIG)
        config.brokerId = getOrGenerateBrokerId(initialMetaPropsEnsemble)
import org.apache.kafka.server.config.ReplicationConfigs
import java.util.Properties
import org.apache.kafka.server.common.MetadataVersion
import org.apache.kafka.server.config.ZkConfigs
import org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig


// After
  private[server] var _brokerId: Int = getInt(ServerConfigs.BROKER_ID_CONFIG)
  def brokerId: Int = _brokerId
  private[server] var _nodeId: Int = getInt(KRaftConfigs.NODE_ID_CONFIG)
  def nodeId: Int = _nodeId
        config._brokerId = getOrGenerateBrokerId(initialMetaPropsEnsemble)
        // Currently, we are migrating from ZooKeeper to KRaft. If broker.id.generation.enable is set to true,
        // we must ensure that the nodeId synchronizes with the broker.id to prevent the nodeId from being -1,
        // which would result in a failure during the migration.
        config._nodeId = config.brokerId
import org.apache.kafka.server.common.MetadataVersion
import org.apache.kafka.server.config.{ReplicationConfigs, ZkConfigs}
import org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17077 the node.id is inconsistent to broker.id when "broker.id.generation.enable=true". (#16540).

**Test**
Test modified in KafkaServerTest.scala covering the corrected behaviour.
