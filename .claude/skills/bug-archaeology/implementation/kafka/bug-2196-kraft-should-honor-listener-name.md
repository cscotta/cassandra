**Commit**: 8adfdbbde0 · **Ticket**: KAFKA-17256
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      sendDescribeClusterRequestToBoundPortUntilAllBrokersPropagated(cluster.nodes.externalListenerName, (15L, SECONDS))
          assertEquals(cluster.brokers.get(broker.id).socketServer.boundPort(cluster.nodes.externalListenerName), broker.port,
      sendDescribeClusterRequestToBoundPortUntilAllBrokersPropagated(cluster.nodes.externalListenerName, (15L, SECONDS))
      .setSecurityProtocol(securityProtocol)
        private TestKitNodes nodes;
            props.putIfAbsent(SocketServerConfigs.LISTENER_SECURITY_PROTOCOL_MAP_CONFIG,
                "EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT");
            props.putIfAbsent(INTER_BROKER_LISTENER_NAME_CONFIG,
                nodes.interBrokerListenerName().value());
            props.putIfAbsent(KRaftConfigs.CONTROLLER_LISTENER_NAMES_CONFIG, "CONTROLLER");
                return "EXTERNAL://localhost:0,CONTROLLER://localhost:0";

// After
      sendDescribeClusterRequestToBoundPortUntilAllBrokersPropagated(cluster.nodes.brokerListenerName, (15L, SECONDS))
          assertEquals(cluster.brokers.get(broker.id).socketServer.boundPort(cluster.nodes.brokerListenerName), broker.port,
      sendDescribeClusterRequestToBoundPortUntilAllBrokersPropagated(cluster.nodes.brokerListenerName, (15L, SECONDS))
      .setBrokerSecurityProtocol(securityProtocol)
        private final TestKitNodes nodes;
        private final String brokerListenerName;
        private final String controllerListenerName;
        private final String brokerSecurityProtocol;
        private final String controllerSecurityProtocol;
            this.brokerListenerName = nodes.brokerListenerName().value();
            this.controllerListenerName = nodes.controllerListenerName().value();
            this.brokerSecurityProtocol = nodes.brokerListenerProtocol().name;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17256 kraft should honor the listener name and security protocol from clusterconfig (#16824).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
