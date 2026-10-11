**Commit**: c8fe551139 · **Ticket**: KAFKA-19030
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
As described in the JIRA ticket, `controlPlaneRequestChannelOpt` was
removed from KRaft mode, so there's no need to use the metrics prefix
anymore. This change removes `metricNamePrefix` from RequestChannel and the
related files. It also removes `DataPlaneAcceptor#MetricPrefix`, since
`DataPlaneAcceptor`  is the only implementation of `Acceptor`. Since the implementation of KIP-291 is essentially removed, we can also
remove `logAndThreadNamePrefix` and `DataPlaneAcceptor#ThreadPrefix`.

**Before/After**
```java
// Before
                     val metricNamePrefix: String,
  private val requestQueueSizeMetricName = metricNamePrefix.concat(RequestQueueSizeMetric)
  private val responseQueueSizeMetricName = metricNamePrefix.concat(ResponseQueueSizeMetric)
  metricsGroup.newGauge(requestQueueSizeMetricName, () => requestQueue.size)
  metricsGroup.newGauge(responseQueueSizeMetricName, () => {
    metricsGroup.newGauge(responseQueueSizeMetricName, () => processor.responseQueueSize,
    metricsGroup.removeMetric(responseQueueSizeMetricName, Map(ProcessorMetricTag -> processorId.toString).asJava)
  val dataPlaneRequestChannel = new RequestChannel(maxQueuedRequests, DataPlaneAcceptor.MetricPrefix, time, apiVersionManager.newRequestMetrics)
  metricsGroup.newGauge(s"${DataPlaneAcceptor.MetricPrefix}NetworkProcessorAvgIdlePercent", () => SocketServer.this.synchronized {
  metricsGroup.newGauge(s"${DataPlaneAcceptor.MetricPrefix}ExpiredConnectionsKilledCount", () => SocketServer.this.synchronized {
  val ThreadPrefix: String = "data-plane"
  val MetricPrefix: String = ""

// After
  metricsGroup.newGauge(RequestQueueSizeMetric, () => requestQueue.size)
  metricsGroup.newGauge(ResponseQueueSizeMetric, () => {
    metricsGroup.newGauge(ResponseQueueSizeMetric, () => processor.responseQueueSize,
    metricsGroup.removeMetric(ResponseQueueSizeMetric, Map(ProcessorMetricTag -> processorId.toString).asJava)
  val dataPlaneRequestChannel = new RequestChannel(maxQueuedRequests, time, apiVersionManager.newRequestMetrics)
  metricsGroup.newGauge(s"NetworkProcessorAvgIdlePercent", () => SocketServer.this.synchronized {
  metricsGroup.newGauge(s"ExpiredConnectionsKilledCount", () => SocketServer.this.synchronized {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19030 remove metricnameprefix from requestchannel (#19374).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
