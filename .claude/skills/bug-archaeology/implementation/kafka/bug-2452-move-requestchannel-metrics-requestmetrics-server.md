**Commit**: c30615e6d7 · **Ticket**: KAFKA-17430
**Subsystem**: checkstyle/import-control-jmh-benchmarks.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import com.yammer.metrics.core.{Histogram, Meter}
import kafka.utils.{Logging, Pool}
import org.apache.kafka.common.message.ApiMessageType.ListenerType
import org.apache.kafka.common.network.{ClientInformation, Send}
import java.util
import scala.collection.mutable
  class Metrics(enabledApis: Iterable[ApiKeys]) {
    def this(scope: ListenerType) = {
      this(ApiKeys.apisForListener(scope).asScala)
    }

    private val metricsMap = mutable.Map[String, RequestMetrics]()

// After
    <allow pkg="org.apache.kafka.network" />
import kafka.utils.Logging
import org.apache.kafka.common.network.Send
import org.apache.kafka.network.metrics.{RequestChannelMetrics, RequestMetrics}
                metrics: RequestChannelMetrics,
            if (body[FetchRequest].isFromFollower) RequestMetrics.FOLLOW_FETCH_METRIC_NAME
            else RequestMetrics.CONSUMER_FETCH_METRIC_NAME
            Seq(RequestMetrics.VERIFY_PARTITIONS_IN_TXN_METRIC_NAME)
        m.deprecatedRequestRate(header.apiKey, header.apiVersion, context.clientInformation).ifPresent(_.mark())
        m.messageConversionsTimeHist.ifPresent(_.update(Math.round(messageConversionsTimeMs)))
        m.tempMemoryBytesHist.ifPresent(_.update(temporaryMemoryBytes))
```

**How to spot**
When reviewing checkstyle/import-control-jmh-benchmarks.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17430: move requestchannel.metrics/requestmetrics to server module (#17015).

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
