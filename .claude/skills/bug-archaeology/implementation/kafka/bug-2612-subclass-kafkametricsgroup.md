**Commit**: 5fac905749 · **Ticket**: KAFKA-17222
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The method overrides of metricName in KafkaMetricsGroup are no longer required since there's a new constructor that implement this with the same behavior. Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import com.yammer.metrics.core.MetricName
  private val metricsGroup = new KafkaMetricsGroup(this.getClass) {
    // For compatibility, metrics are defined to be under `Log` class
    override def metricName(name: String, tags: util.Map[String, String]): MetricName = {
      KafkaMetricsGroup.explicitMetricName(getClass.getPackage.getName, "Log", name, tags)
    }
  }
  private val blockedPercentMeterMetricName = KafkaMetricsGroup.explicitMetricName(
    "kafka.network",
    "Acceptor",
import com.yammer.metrics.core.MetricName
import java.util

// After
  // For compatibility, metrics are defined to be under `Log` class
  private val metricsGroup = new KafkaMetricsGroup(getClass.getPackage.getName, "Log")
  private val backwardCompatibilityMetricGroup = new KafkaMetricsGroup("kafka.network", "Acceptor")
  private val blockedPercentMeterMetricName = backwardCompatibilityMetricGroup.metricName(
  // For backwards compatibility, we need to keep older metrics tied
  // to their original name when this class was named `KafkaServer`
  private val metricsGroup = new KafkaMetricsGroup(Server.MetricsPrefix, KafkaBroker.MetricsTypeName)
  private val metricsGroup = new KafkaMetricsGroup("kafka.server","BrokerMetadataListener")
  private val batchProcessingTimeHistName = metricsGroup.metricName("MetadataBatchProcessingTimeUs", Collections.emptyMap())
  private val batchSizeHistName = metricsGroup.metricName("MetadataBatchSizes", Collections.emptyMap())
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17222 remove the subclass of kafkametricsgroup (#16752).

**Test**
Test modified in KafkaMetricsGroupTest.scala covering the corrected behaviour.
