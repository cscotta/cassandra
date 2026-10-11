**Commit**: d3936365bf · **Ticket**: KAFKA-16468
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When brokers undergoing ZK migration register with the controller, it should verify that they have
provided a way to contact them via their inter.broker.listener. Otherwise the migration will fail
later on with a more confusing error message. Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
                brokerUncleanShutdownHandler
        BrokerUncleanShutdownHandler brokerUncleanShutdownHandler

// After
          setInterBrokerListenerName(config.interBrokerListenerName.value()).
import java.util
      .setListeners(new BrokerRegistrationRequestData.ListenerCollection(util.Arrays.asList(
        new BrokerRegistrationRequestData.Listener().
          setName("EXTERNAL").
          setHost("example.com").
          setPort(8082).
          setSecurityProtocol(SecurityProtocol.PLAINTEXT.id))
            .iterator()))
        private String interBrokerListenerName = "PLAINTEXT";
        Builder setInterBrokerListenerName(String interBrokerListenerName) {
            this.interBrokerListenerName = interBrokerListenerName;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16468: verify that migrating brokers provide their inter.broker.listener (#17159).

**Test**
Test modified in BrokerRegistrationRequestTest.scala covering the corrected behaviour.
