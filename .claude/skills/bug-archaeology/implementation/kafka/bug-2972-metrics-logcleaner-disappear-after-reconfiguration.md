**Commit**: e33eb82fed · **Ticket**: KAFKA-16574
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Gaurav Narula <gaurav_narula2@apple.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  def shutdown(): Unit = {
      cleaners.foreach(_.shutdown())
      cleaners.clear()

    shutdown()
import org.apache.kafka.server.metrics.KafkaMetricsGroup


// After
  private[this] def shutdownCleaners(): Unit = {
    cleaners.foreach(_.shutdown())
    cleaners.clear()
  }

  /**
   * Stop the background cleaner threads
   */
  def shutdown(): Unit = {
      shutdownCleaners()
    // call shutdownCleaners() instead of shutdown to avoid unnecessary deletion of metrics
    shutdownCleaners()
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16574 the metrics of logcleaner disappear after reconfiguration (#15863).

**Test**
Test modified in LogCleanerTest.scala covering the corrected behaviour.
