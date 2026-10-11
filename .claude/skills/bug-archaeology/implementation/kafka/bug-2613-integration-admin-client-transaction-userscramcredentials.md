**Commit**: 46f1f0268b · **Ticket**: KAFKA-17137
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.{AfterEach, BeforeEach, Disabled, TestInfo}

// After
import org.apache.kafka.common.internals.Topic
import org.junit.jupiter.api.{AfterEach, BeforeEach, Disabled, TestInfo, Timeout}
  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft"))
  def testDescribeUserScramCredentials(quorum: String): Unit = {
    client = createAdminClient

    // add a new user
    val targetUserName = "tom"
    client.alterUserScramCredentials(Collections.singletonList(
      new UserScramCredentialUpsertion(targetUserName, new ScramCredentialInfo(ScramMechanism.SCRAM_SHA_256, 4096), "123456")
    )).all.get
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17137 add integration tests for admin client (transaction and userscramcredentials related) (#16652).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
