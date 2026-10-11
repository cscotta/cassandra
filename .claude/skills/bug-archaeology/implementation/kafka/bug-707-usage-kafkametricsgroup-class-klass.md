**Commit**: 25de320951 · **Ticket**: KAFKA-19518
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
the constructor is error-prone when migrating code, since metrics could
get unintentionally changed. We should remove the constructor and use
constant strings instead to avoid issues like KAFKA-17876, KAFKA-19150,
and others. Reviewers: Ken Huang <s7133700@gmail.com>, Jhen-Yung Hsu
 <jhenyunghsu@gmail.com>, KuoChe <kuoche1712003@gmail.com>, Chia-Ping
 Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  private val metricsGroup = new KafkaMetricsGroup(classOf[Partition])

  private val metricsGroup = new KafkaMetricsGroup(this.getClass)

  private val metricsGroup = new KafkaMetricsGroup(this.getClass)
  private val metricsGroup = new KafkaMetricsGroup(this.getClass)

  private val metricsGroup = new KafkaMetricsGroup(this.getClass)

  private val metricsGroup = new KafkaMetricsGroup(this.getClass)


// After
  // Changing the package or class name may cause incompatibility with existing code and metrics configuration
  private val metricsPackage = "kafka.cluster"
  private val metricsClassName = "Partition"
  private val metricsGroup = new KafkaMetricsGroup(metricsPackage, metricsClassName)
  // Changing the package or class name may cause incompatibility with existing code and metrics configuration
  private val metricsPackage = "kafka.coordinator.transaction"
  private val metricsClassName = "TransactionMarkerChannelManager"
  private val metricsGroup = new KafkaMetricsGroup(metricsPackage, metricsClassName)
  // Changing the package or class name may cause incompatibility with existing code and metrics configuration
  private val metricsPackage = "kafka.log"
  private val metricsClassName = "LogManager"
  private val metricsGroup = new KafkaMetricsGroup(metricsPackage, metricsClassName)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19518 remove the usage of kafkametricsgroup(class<?> klass) (#20399).

**Test**
Test modified in MetricsTest.scala covering the corrected behaviour.
