**Commit**: 901c656a1d · **Ticket**: KAFKA-15746
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.{BeforeEach, Test, TestInfo}
  @Test
  def testSetUnsetQuota(): Unit = {
  @Test
  def testQuotaMetric(): Unit = {
  @Test
  def testStrictCreateTopicsRequest(): Unit = {
  @Test
  def testPermissiveCreateTopicsRequest(): Unit = {
  @Test
  def testUnboundedCreateTopicsRequest(): Unit = {
  @Test

// After
import org.apache.kafka.common.network.ListenerName
import org.junit.jupiter.api.{BeforeEach, TestInfo}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import scala.collection.Seq
  override def kraftControllerConfigs(): Seq[Properties] = {
    val props = super.kraftControllerConfigs()
    props.head.setProperty(QuotaConfigs.NUM_CONTROLLER_QUOTA_SAMPLES_CONFIG, ControllerQuotaSamples.toString)
    props
  }

  @ParameterizedTest
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15746: kraft support in controllermutationquotatest (#16620).

**Test**
Test modified in ControllerMutationQuotaTest.scala covering the corrected behaviour.
