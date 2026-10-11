**Commit**: 53d8316b5d · **Ticket**: KAFKA-14934
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.RaftClusterInvocationContext;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
        RaftClusterInvocationContext.RaftClusterInstance raftInstance =
                (RaftClusterInvocationContext.RaftClusterInstance) cluster;
                raftInstance.brokers().get(0).logManager()
        raftInstance.brokers().get(0).shutdown();
        raftInstance.brokers().get(0).startup();
        assertDoesNotThrow(() -> raftInstance.getUnderlying().fatalFaultHandler().maybeRethrowFirstException());
import org.apache.kafka.common.test.api.RaftClusterInvocationContext.RaftClusterInstance
    val raftCluster = cluster.asInstanceOf[RaftClusterInstance]
    val sourceBroker = raftCluster.brokers.values().stream().findFirst().get().asInstanceOf[BrokerServer]
    val controllerServer = raftCluster.controllers.values().stream()

// After
                cluster.brokers().get(0).logManager()
        cluster.brokers().get(0).shutdown();
        cluster.brokers().get(0).startup();
        assertTrue(cluster.firstFatalException().isEmpty());
    val sourceBroker = cluster.brokers.values().stream().findFirst().get().asInstanceOf[BrokerServer]
    val controllerServer = cluster.controllers.values().stream()
    val sourceBroker = cluster.brokers.values().stream().findFirst().get().asInstanceOf[BrokerServer]
    val controllerServer = cluster.controllers().values().stream()
```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14934 kafkaclustertestkit makes faulthandler accessible (#17774).

**Test**
Test modified in LogManagerIntegrationTest.java covering the corrected behaviour.
