**Commit**: 148be48785 · **Ticket**: KAFKA-20012
**Subsystem**: core/src
**Category**: Missing override of default method

**Root cause**
Migrates `NodeToControllerChannelManager` and related classes from Scala
(core module) to Java (server module). New Java Classes (server module)  include `ControllerInformation`,
`RaftControllerNodeProvider`, `NodeToControllerChannelManagerImpl`,
`NodeToControllerRequestThread` and `NodeToControllerQueueItem`. In `AbstractKafkaConfig`,  added
`addReconfigurable()`/`removeReconfigurable()` abstract methods to
support  migration. In `KafkaConfig`, added `override` keywords for
reconfigurable methods.

**Before/After**
```java
// Before
    controllerNodeProvider: ControllerNodeProvider,
      time = time,
      metrics = metrics,
      config = config,
      channelName = "alter-partition",
      threadNamePrefix = threadNamePrefix,
      retryTimeoutMs = Long.MaxValue
    // We will not timeout AlterPartition request, instead letting it retry indefinitely
              // For now we treat authentication errors as retriable. We use the
      val controllerNodeProvider = RaftControllerNodeProvider(raftManager, config)
        channelName = "forwarding",
        retryTimeoutMs = 60000

// After
import org.apache.kafka.server.{ControllerInformation, NodeToControllerChannelManagerImpl}
import java.util.function.Supplier
    controllerNodeProvider: Supplier[ControllerInformation],
      time,
      metrics,
      config,
      "alter-partition",
      threadNamePrefix,
      Long.MaxValue
    // We will not time out AlterPartition request, instead letting it retry indefinitely
              // For now, we treat authentication errors as retriable. We use the
import org.apache.kafka.server.NodeToControllerChannelManagerImpl
```

**How to spot**
When reviewing core/src code, look for missing override of default method patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20012 move nodetocontrollerchannelmanagerimpl to server module (#21261).

**Test**
Test modified in NodeToControllerRequestThreadTest.scala covering the corrected behaviour.
