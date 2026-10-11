**Commit**: f6f41dc5eb · **Ticket**: KAFKA-17631
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.api.SaslSetup
import kafka.security.JaasTestUtils
import kafka.server.SaslApiVersionsRequestTest.{kafkaClientSaslMechanism, kafkaServerSaslMechanisms}
import org.apache.kafka.common.test.api.{ClusterConfig, ClusterTemplate, Type}
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.internals.BrokerSecurityConfigs
import org.apache.kafka.network.SocketServerConfigs
import org.junit.jupiter.api.{AfterEach, BeforeEach, Disabled}
object SaslApiVersionsRequestTest {
  val kafkaClientSaslMechanism = "PLAIN"
  val kafkaServerSaslMechanisms: Seq[String] = List("PLAIN")
  val controlPlaneListenerName = "CONTROL_PLANE"

// After
import org.apache.kafka.common.test.api.{ClusterTest, Type}
import org.apache.kafka.common.test.junit.ClusterTestExtensions
import org.junit.jupiter.api.extension.ExtendWith
@ExtendWith(value = Array(classOf[ClusterTestExtensions]))
  @ClusterTest(types = Array(Type.KRAFT),
    brokerSecurityProtocol = SecurityProtocol.SASL_PLAINTEXT,
    controllerSecurityProtocol = SecurityProtocol.SASL_PLAINTEXT
  )
      validateApiVersionsResponse(
        apiVersionsResponse,
          cluster.config().serverProperties().get("unstable.api.versions.enable")),
        apiVersion = 0.toShort
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17631 convert saslapiversionsrequesttest to kraft (#18330).

**Test**
Test modified in SaslApiVersionsRequestTest.scala covering the corrected behaviour.
