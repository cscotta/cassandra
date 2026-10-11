**Commit**: d0dfefbe63 · **Ticket**: KAFKA-15727
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
his PR adds KRaft support to the following tests in AlterUserScramCredentialsRequestNotAuthorizedTest

Co-authored-by: Manikumar Reddy <manikumar.reddy@gmail.com>

Reviewers:  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.Test
    properties.put(ServerConfigs.AUTHORIZER_CLASS_NAME_CONFIG, classOf[AlterCredentialsTest.TestAuthorizer].getName)
  @Test
  def testAlterNothingNotAuthorized(): Unit = {
  @Test
  def testAlterSomethingNotAuthorized(): Unit = {
  private def sendAlterUserScramCredentialsRequest(request: AlterUserScramCredentialsRequest, socketServer: SocketServer = controllerSocketServer): AlterUserScramCredentialsResponse = {
      this.serverConfig.setProperty(ServerConfigs.AUTHORIZER_CLASS_NAME_CONFIG, classOf[StandardAuthorizer].getName)

// After
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
    if(isKRaftTest())
      properties.put(ServerConfigs.AUTHORIZER_CLASS_NAME_CONFIG, classOf[AlterCredentialsTest.TestStandardAuthorizer].getName)
    else {
      properties.put(ServerConfigs.AUTHORIZER_CLASS_NAME_CONFIG,  classOf[AlterCredentialsTest.TestAuthorizer].getName)
    }
  override def kraftControllerConfigs(): collection.Seq[Properties] = {
    val controllerConfigs = super.kraftControllerConfigs()
    controllerConfigs.head.put(ServerConfigs.AUTHORIZER_CLASS_NAME_CONFIG, classOf[AlterCredentialsTest.TestStandardAuthorizer].getName)
    controllerConfigs.head.put(BrokerSecurityConfigs.PRINCIPAL_BUILDER_CLASS_CONFIG, classOf[AlterCredentialsTest.TestPrincipalBuilderReturningUnauthorized].getName)
    controllerConfigs
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15727: added kraft support in alteruserscramcredentialsrequestnotauthorizedtest (#15224).

**Test**
Test modified in AlterUserScramCredentialsRequestNotAuthorizedTest.scala covering the corrected behaviour.
