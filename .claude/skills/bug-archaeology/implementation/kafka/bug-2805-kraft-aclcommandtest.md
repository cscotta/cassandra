**Commit**: 7fe3374f7a · **Ticket**: KAFKA-15713
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

        listAcls(adminClient)

        listAcls(adminClient)

        listAcls()

        listAcls()
import kafka.server.{KafkaConfig, KafkaServer, QuorumTestHarness}
import org.apache.kafka.common.acl.{AccessControlEntry, AclOperation, AclPermissionType}
  var servers: Seq[KafkaServer] = Seq()
    brokerProps = TestUtils.createBrokerConfig(0, zkConnect)

// After
import kafka.server.{KafkaBroker, KafkaConfig, QuorumTestHarness}
import org.apache.kafka.common.acl.{AccessControlEntry, AclOperation, AclPermissionType}
import org.apache.kafka.metadata.authorizer.StandardAuthorizer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
  var servers: Seq[KafkaBroker] = Seq()
    brokerProps = TestUtils.createBrokerConfig(0, zkConnectOrNull)
    if (isKRaftTest()) {
      brokerProps.putAll(kraftControllerConfigs().head)
    } else {
      brokerProps.put(ServerConfigs.AUTHORIZER_CLASS_NAME_CONFIG, classOf[AclAuthorizer].getName)
      brokerProps.put(AclAuthorizer.SuperUsersProp, "User:ANONYMOUS")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15713 kraft support in aclcommandtest (#15830).

**Test**
Test modified in AclCommandTest.scala covering the corrected behaviour.
