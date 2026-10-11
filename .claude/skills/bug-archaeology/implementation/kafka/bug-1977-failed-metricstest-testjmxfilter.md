**Commit**: ccbb73111c · **Ticket**: KAFKA-18078
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.metrics.KafkaMetricsReporter
import kafka.utils.{CoreUtils, Logging, Mx4jLoader, VerifiableProperties}
import org.apache.kafka.server.metrics.KafkaYammerMetrics
  KafkaMetricsReporter.startReporters(VerifiableProperties(config.originals))
  KafkaYammerMetrics.INSTANCE.configure(config.originals)
import kafka.utils.{CoreUtils, Logging}

// After
import kafka.utils.{CoreUtils, Logging, Mx4jLoader}
import kafka.metrics.KafkaMetricsReporter
import kafka.utils.{CoreUtils, Logging, VerifiableProperties}
  KafkaMetricsReporter.startReporters(VerifiableProperties(sharedServerConfig.originals))
  KafkaYammerMetrics.INSTANCE.configure(sharedServerConfig.originals)

```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18078 fix failed metricstest.testjmxfilter (#17924).

**Test**
No test added.
