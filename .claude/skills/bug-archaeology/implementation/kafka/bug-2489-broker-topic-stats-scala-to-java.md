**Commit**: b9fe9f532f · **Ticket**: KAFKA-16972
**Subsystem**: server/BrokerTopicStats
**Category**: Wrong constant / default value

**Root cause**
`BrokerTopicStats` lived in the `kafka.server` Scala package, which made it hard to reference from Java modules and required Scala-to-Java interop boilerplate at every call site. Because the class manages per-topic JMX metrics with no Scala-specific logic, it was straightforward to migrate to a pure Java implementation in the `org.apache.kafka.storage.log.metrics` package. The migration also eliminates the `kafka.server` package dependency for components that only need topic-level metrics.

**Before/After**
```scala
// Before (Scala, kafka.server package)
class BrokerTopicStats extends KafkaMetricsGroup {
  val allTopicsStats = new BrokerTopicMetrics(None)
}

// After (Java, org.apache.kafka.storage.log.metrics package)
public class BrokerTopicStats {
    private final BrokerTopicMetrics allTopicsStats = new BrokerTopicMetrics(Optional.empty());
}
```

**How to spot**
Scala classes used heavily from Java modules are candidates for migration when they contain no Scala-specific constructs. After migration, verify that all import statements, dependency declarations, and test fixtures referencing the old package are updated to the new one.

**Test**
`BrokerTopicStatsTest` rewritten in Java and moved to the new module.
