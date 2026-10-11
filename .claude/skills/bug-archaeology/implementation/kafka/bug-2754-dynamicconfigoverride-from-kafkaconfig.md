**Commit**: 4a49a1249d · **Ticket**: KAFKA-17059
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    currentConfig = new KafkaConfig(kafkaConfig.props, false, None)
          val newConfig = new KafkaConfig(kafkaProps.asJava, false, None)
    val newConfig = new KafkaConfig(newProps.asJava, doLog, None)
class KafkaConfig private(doLog: Boolean, val props: java.util.Map[_, _], dynamicConfigOverride: Option[DynamicBrokerConfig])
  def this(props: java.util.Map[_, _]) = this(true, KafkaConfig.populateSynonyms(props), None)
  def this(props: java.util.Map[_, _], doLog: Boolean) = this(doLog, KafkaConfig.populateSynonyms(props), None)
  def this(props: java.util.Map[_, _], doLog: Boolean, dynamicConfigOverride: Option[DynamicBrokerConfig]) =
    this(doLog, KafkaConfig.populateSynonyms(props), dynamicConfigOverride)
  private[server] val dynamicConfig = dynamicConfigOverride.getOrElse(new DynamicBrokerConfig(this))
  val brokerConfig = new KafkaConfig(sharedServerConfig.props, false, None)
  val controllerConfig = new KafkaConfig(sharedServerConfig.props, false, None)
import scala.Option;

// After
    currentConfig = new KafkaConfig(kafkaConfig.props, false)
          val newConfig = new KafkaConfig(kafkaProps.asJava, false)
    val newConfig = new KafkaConfig(newProps.asJava, doLog)
class KafkaConfig private(doLog: Boolean, val props: util.Map[_, _])
  def this(props: java.util.Map[_, _]) = this(true, KafkaConfig.populateSynonyms(props))
  def this(props: java.util.Map[_, _], doLog: Boolean) = this(doLog, KafkaConfig.populateSynonyms(props))
  private[server] val dynamicConfig = new DynamicBrokerConfig(this)
  val brokerConfig = new KafkaConfig(sharedServerConfig.props, false)
  val controllerConfig = new KafkaConfig(sharedServerConfig.props, false)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17059 remove `dynamicconfigoverride` from kafkaconfig (#16523).

**Test**
Test modified in KafkaClusterTestKit.java covering the corrected behaviour.
