**Commit**: 059589ef97 · **Ticket**: KAFKA-19606
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
This change implements a global shared thread counter mechanism to
properly calculate the RequestHandlerAvgIdlePercent metric across all
KafkaRequestHandlerPool instances within the same JVM process. This
ensures accurate idle percentage calculations, especially in combined
mode where both broker and controller request handler pools coexist. Previously, each KafkaRequestHandlerPool calculated idle percentages
independently using only its own thread count as the denominator. In
combined mode, this led to inaccurate aggregate idle percentage
calculations and potential metric values exceeding 100% (values > 1.0)

Added aggregateThreads as a global AtomicInteger in
KafkaRequestHandlerPoolFactory...

**Before/After**
```java
// Before
      dataPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.nodeId,
        socketServer.dataPlaneRequestChannel, dataPlaneRequestProcessor, time,
        config.numIoThreads, "RequestHandlerAvgIdlePercent")
      controllerApisHandlerPool = new KafkaRequestHandlerPool(config.nodeId,
        "RequestHandlerAvgIdlePercent",
        "controller")
  val totalHandlerThreads: AtomicInteger,
  nodeName: String = "broker"
      aggregateIdleMeter.mark(idleTime / totalHandlerThreads.get)
  requestHandlerAvgIdleMetricName: String,


// After
      dataPlaneRequestHandlerPool = sharedServer.requestHandlerPoolFactory.createPool(
        config.nodeId,
        socketServer.dataPlaneRequestChannel,
        dataPlaneRequestProcessor,
        time,
        config.numIoThreads,
        "broker"
      )
      controllerApisHandlerPool = sharedServer.requestHandlerPoolFactory.createPool(
        config.nodeId,
        "controller"
      )
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19606; implement request handler metrics defined in kip-1207 (#20481).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
