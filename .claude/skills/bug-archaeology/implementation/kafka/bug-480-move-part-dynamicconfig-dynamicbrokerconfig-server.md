**Commit**: 02d568c83c · **Ticket**: KAFKA-18854
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
… module

This is moving just enough logic so ConfigCommand does not depend on any
core classes anymore and can be moved to the tools module. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.DynamicConfig
import org.apache.kafka.server.config.{ConfigType, QuotaConfig}
import kafka.network.SocketServer._
import org.apache.kafka.network.{ConnectionQuotaEntity, ConnectionThrottledException, SocketServerConfigs, TooManyConnectionsException}
  private val memoryPoolDepletedPercentMetricName = metrics.metricName("MemoryPoolAvgDepletedPercent", MetricsGroup)
  private val memoryPoolDepletedTimeMetricName = metrics.metricName("MemoryPoolDepletedTimeTotal", MetricsGroup)
      metrics.metricName("io-wait-ratio", MetricsGroup, p.metricTags)
      metrics.metricName("expired-connections-killed-count", MetricsGroup, p.metricTags)
  override def reconfigurableConfigs: Set[String] = SocketServer.ReconfigurableConfigs
object SocketServer {
  val MetricsGroup = "socket-server-metrics"


// After
import org.apache.kafka.server.config.{ConfigType, DynamicConfig, QuotaConfig}
import org.apache.kafka.network.{ConnectionQuotaEntity, ConnectionThrottledException, SocketServer => JSocketServer, SocketServerConfigs, TooManyConnectionsException}
  private val memoryPoolDepletedPercentMetricName = metrics.metricName("MemoryPoolAvgDepletedPercent", JSocketServer.METRICS_GROUP)
  private val memoryPoolDepletedTimeMetricName = metrics.metricName("MemoryPoolDepletedTimeTotal", JSocketServer.METRICS_GROUP)
      metrics.metricName("io-wait-ratio", JSocketServer.METRICS_GROUP, p.metricTags)
      metrics.metricName("expired-connections-killed-count", JSocketServer.METRICS_GROUP, p.metricTags)
  override def reconfigurableConfigs: util.Set[String] = JSocketServer.RECONFIGURABLE_CONFIGS
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18854: move part of dynamicconfig/dynamicbrokerconfig to server… (#21302).

**Test**
Test modified in ConnectionQuotasTest.scala covering the corrected behaviour.
