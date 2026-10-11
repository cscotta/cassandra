**Commit**: d00f0ecf1a · **Ticket**: KAFKA-18124
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{KRaftConfigs, ZkConfigs}
import org.apache.kafka.server.config.ReplicationConfigs
import scala.jdk.CollectionConverters._

    if (hasBrokerRole || hasControllerRole) { // KRaft
      props.setProperty(KRaftConfigs.CONTROLLER_LISTENER_NAMES_CONFIG, "SASL_PLAINTEXT")
      if (hasBrokerRole && hasControllerRole) {
        props.setProperty(SocketServerConfigs.LISTENERS_CONFIG, s"$brokerListener,$controllerListener")
      } else if (hasControllerRole) {
        props.setProperty(SocketServerConfigs.LISTENERS_CONFIG, controllerListener)
      } else if (hasBrokerRole) {
        props.setProperty(SocketServerConfigs.LISTENERS_CONFIG, brokerListener)

// After
import org.apache.kafka.server.config.{KRaftConfigs, ReplicationConfigs, ZkConfigs}
    props.setProperty(KRaftConfigs.CONTROLLER_LISTENER_NAMES_CONFIG, "SASL_PLAINTEXT")
    if (hasBrokerRole && hasControllerRole) {
      props.setProperty(SocketServerConfigs.LISTENERS_CONFIG, s"$brokerListener,$controllerListener")
    } else if (hasControllerRole) {
      props.setProperty(SocketServerConfigs.LISTENERS_CONFIG, controllerListener)
    } else if (hasBrokerRole) {
      props.setProperty(SocketServerConfigs.LISTENERS_CONFIG, brokerListener)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18124 remove zk migration from `raftmanagertest`, `brokerlifecyclemanagertest`, `kafkaconfigtest`, and `replicaman.

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
