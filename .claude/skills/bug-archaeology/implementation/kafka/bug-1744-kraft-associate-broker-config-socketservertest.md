**Commit**: 03f16f368b · **Ticket**: KAFKA-18313
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  val props = TestUtils.createBrokerConfig(0, TestUtils.MockZkConnect, port = 0)
  @Test
  def testStagedListenerStartup(): Unit = {
    shutdownServerAndMetrics(server)
    val testProps = new Properties
    testProps ++= props
    testProps.put("listeners", "EXTERNAL://localhost:0,INTERNAL://localhost:0,CONTROL_PLANE://localhost:0")
    testProps.put("listener.security.protocol.map", "EXTERNAL:PLAINTEXT,INTERNAL:PLAINTEXT,CONTROL_PLANE:PLAINTEXT")
    testProps.put("control.plane.listener.name", "CONTROL_PLANE")
    testProps.put("inter.broker.listener.name", "INTERNAL")
    val config = KafkaConfig.fromProps(testProps)
    val testableServer = new TestableSocketServer(config)

// After
  val props = TestUtils.createBrokerConfig(0, null, port = 0)
    testProps.put("listeners", "EXTERNAL://localhost:0,INTERNAL://localhost:0")
    testProps.put("advertised.listeners", "EXTERNAL://localhost:0,INTERNAL://localhost:0")
    testProps.put("controller.listener.names", "CONTROLLER")
    val newProps = TestUtils.createBrokerConfig(0, null, port = 0)
    val overrideProps = TestUtils.createBrokerConfig(0, null, port = 0)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18313 fix to kraft or remove tests associate with zk broker config in socketservertest and replicafetcherthreadtes.

**Test**
Test modified in SocketServerTest.scala covering the corrected behaviour.
