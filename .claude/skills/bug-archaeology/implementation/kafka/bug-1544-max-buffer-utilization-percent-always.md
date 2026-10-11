**Commit**: 356f0d815c · **Ticket**: KAFKA-18597
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
   * @return the max value (int value) or 0 if there is no cleaner
  private[log] def maxOverCleanerThreads(f: CleanerThread => Double): Int =
    cleaners.map(f).maxOption.getOrElse(0.0d).toInt
    () => maxOverCleanerThreads(_.lastStats.bufferUtilization) * 100)
  metricsGroup.newGauge(MaxCleanTimeMetricName, () => maxOverCleanerThreads(_.lastStats.elapsedSecs))
    () => maxOverCleanerThreads(_.lastPreCleanStats.maxCompactionDelayMs.toDouble) / 1000)
  private val MaxBufferUtilizationPercentMetricName = "max-buffer-utilization-percent"
  private val MaxCleanTimeMetricName = "max-clean-time-secs"
  private val MaxCompactionDelayMetricsName = "max-compaction-delay-secs"
  def testMaxOverCleanerThreads(): Unit = {
    val logCleaner =  new LogCleaner(new CleanerConfig(true),
      time = time)

// After
   * @return the max value or 0 if there is no cleaner
  private[log] def maxOverCleanerThreads(f: CleanerThread => Double): Double =
    cleaners.map(f).maxOption.getOrElse(0.0d)
    () => (maxOverCleanerThreads(_.lastStats.bufferUtilization) * 100).toInt)
  metricsGroup.newGauge(MaxCleanTimeMetricName, () => maxOverCleanerThreads(_.lastStats.elapsedSecs).toInt)
    () => (maxOverCleanerThreads(_.lastPreCleanStats.maxCompactionDelayMs.toDouble) / 1000).toInt)
  // Visible for test.
  private[log] val MaxBufferUtilizationPercentMetricName = "max-buffer-utilization-percent"
  private[log] val MaxCleanTimeMetricName = "max-clean-time-secs"
  private[log] val MaxCompactionDelayMetricsName = "max-compaction-delay-secs"
import kafka.log.LogCleaner.{MaxBufferUtilizationPercentMetricName, MaxCleanTimeMetricName, MaxCompactionDelayMetricsName}
  def testMaxBufferUtilizationPercentMetric(): Unit = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18597 fix max-buffer-utilization-percent is always 0 (#18627).

**Test**
Test modified in LogCleanerTest.scala covering the corrected behaviour.
