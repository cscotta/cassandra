**Commit**: 9f67ee3a9d · **Ticket**: KAFKA-18305
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When inter.broker.listener is explicitly set, validate that it is not in the set of controller.listener.names. Reviewers: Colin P. McCabe <cmccabe@apache.org>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    props.put(KRaftConfigs.CONTROLLER_LISTENER_NAMES_CONFIG, "PLAINTEXT")

// After
      // controller.listener.names must not contain inter.broker.listener.name when inter.broker.listener.name is explicitly set
      if (Option(getString(ReplicationConfigs.INTER_BROKER_LISTENER_NAME_CONFIG)).isDefined) {
        require(
          !controllerListenerNames.contains(interBrokerListenerName.value()),
          s"${KRaftConfigs.CONTROLLER_LISTENER_NAMES_CONFIG} must not contain an explicitly set ${ReplicationConfigs.INTER_BROKER_LISTENER_NAME_CONFIG} configuration value when ${KRaftConfigs.PROCESS_ROLES_CONFIG}=controller'"
        )
      }
import org.apache.kafka.network.SocketServerConfigs
    props.put(KRaftConfigs.CONTROLLER_LISTENER_NAMES_CONFIG, "CONTROLLER")
    props.put(SocketServerConfigs.LISTENERS_CONFIG, "CONTROLLER://:9092")
  @Test
  def testControllerListenerNamesValidForKRaftControllerOnly(): Unit = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18305: validate controller.listener.names is not in inter.broker.listener.name for kcontrollers (#18222).

**Test**
Test modified in ControllerApisTest.scala covering the corrected behaviour.
