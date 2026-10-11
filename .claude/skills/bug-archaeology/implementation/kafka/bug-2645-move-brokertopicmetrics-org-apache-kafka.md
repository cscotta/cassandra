**Commit**: 010ab19b72 · **Ticket**: KAFKA-16972
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.{BrokerTopicMetrics, BrokerTopicStats, RequestLocal}
import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, TimeUnit}
import java.util.Collections
import scala.jdk.CollectionConverters._
class BrokerTopicMetrics(name: Option[String], remoteStorageEnabled: Boolean = false) {
  private val metricsGroup = new KafkaMetricsGroup(this.getClass)

  val tags: java.util.Map[String, String] = name match {
    case None => Collections.emptyMap()
    case Some(topic) => Map("topic" -> topic).asJava
  }


// After
    <subpackage name="storage.log">
        <allow pkg="org.apache.kafka.server" />
        <allow pkg="com.yammer.metrics" />
    </subpackage>

import kafka.server.{BrokerTopicStats, RequestLocal}
import org.apache.kafka.storage.log.metrics.BrokerTopicMetrics
import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.apache.kafka.storage.log.metrics.BrokerTopicMetrics
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16972 move brokertopicmetrics to org.apache.kafka.storage.log.metrics (#16387).

**Test**
Test modified in KafkaRequestHandlerTest.scala covering the corrected behaviour.
