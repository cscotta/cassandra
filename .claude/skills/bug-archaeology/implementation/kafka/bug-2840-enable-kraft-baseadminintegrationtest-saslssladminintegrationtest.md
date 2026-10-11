**Commit**: 166d9e8059 · **Ticket**: KAFKA-15751
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test, TestInfo, Timeout}
  @Test
  def testCreateDeleteTopics(): Unit = {
  @Test
  def testAuthorizedOperations(): Unit = {
import kafka.utils.{JaasTestUtils, TestUtils}
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test, TestInfo, Timeout}

  val aclAuthorizerClassName: String = classOf[AclAuthorizer].getName
  def kafkaPrincipal = new KafkaPrincipal(KafkaPrincipal.USER_TYPE, JaasTestUtils.KafkaServerPrincipalUnqualifiedName)

    this.serverConfig.setProperty(ServerConfigs.AUTHORIZER_CLASS_NAME_CONFIG, aclAuthorizerClassName)

// After
import org.junit.jupiter.api.{AfterEach, BeforeEach , TestInfo, Timeout}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft"))
  def testCreateDeleteTopics(quorum: String): Unit = {
  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft"))
  def testAuthorizedOperations(quorum: String): Unit = {
import kafka.utils.{JaasTestUtils, TestInfoUtils, TestUtils}
import org.apache.kafka.metadata.authorizer.StandardAuthorizer
import org.junit.jupiter.api.{AfterEach, BeforeEach, TestInfo, Timeout}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15751, kafka-15752: enable kraft for baseadminintegrationtest and saslssladminintegrationtest (#15175).

**Test**
Test modified in BaseAdminIntegrationTest.scala covering the corrected behaviour.
