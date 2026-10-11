**Commit**: 095bd0a6d4 · **Ticket**: KAFKA-18101
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ziming Deng<dengziming1993@gmail.com>,  Chia-Ping Tsai<chia7712@gmail.com>,  TaiJuWu<tjwu1217@gmail.com>.

**Before/After**
```java
// Before
import kafka.utils.TestUtils.assertFutureExceptionTypeEquals
    assertFutureExceptionTypeEquals(alterResult.values.get(topicResource1), classOf[PolicyViolationException])
    assertFutureExceptionTypeEquals(alterResult.values.get(topicResource3), classOf[InvalidConfigurationException])
    assertFutureExceptionTypeEquals(alterResult.values.get(brokerResource), classOf[InvalidRequestException])
    assertFutureExceptionTypeEquals(alterResult.values.get(topicResource1), classOf[PolicyViolationException])
    assertFutureExceptionTypeEquals(alterResult.values.get(topicResource3), classOf[InvalidConfigurationException])
    assertFutureExceptionTypeEquals(alterResult.values.get(brokerResource), classOf[InvalidRequestException])
    TestUtils.assertFutureExceptionTypeEquals(result.describedGroups().get(group), classOf[GroupAuthorizationException])
    TestUtils.assertFutureExceptionTypeEquals(result.deletedGroups().get(group), classOf[GroupAuthorizationException])
    TestUtils.assertFutureExceptionTypeEquals(result.deletedGroups().get(group), classOf[GroupAuthorizationException])

// After
import org.apache.kafka.test.TestUtils.assertFutureThrows
    assertFutureThrows(alterResult.values.get(topicResource1), classOf[PolicyViolationException])
    assertFutureThrows(alterResult.values.get(topicResource3), classOf[InvalidConfigurationException])
    assertFutureThrows(alterResult.values.get(brokerResource), classOf[InvalidRequestException])
    assertFutureThrows(alterResult.values.get(topicResource1), classOf[PolicyViolationException])
    assertFutureThrows(alterResult.values.get(topicResource3), classOf[InvalidConfigurationException])
    assertFutureThrows(alterResult.values.get(brokerResource), classOf[InvalidRequestException])
    JTestUtils.assertFutureThrows(result.describedGroups().get(group), classOf[GroupAuthorizationException])
    JTestUtils.assertFutureThrows(result.deletedGroups().get(group), classOf[GroupAuthorizationException])
    JTestUtils
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18101: merge duplicate assertfuturethrows and assertfutureexceptiontypeequals (#17991).

**Test**
Test modified in AdminClientWithPoliciesIntegrationTest.scala covering the corrected behaviour.
