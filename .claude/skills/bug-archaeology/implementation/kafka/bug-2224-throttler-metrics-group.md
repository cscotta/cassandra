**Commit**: dcd3bbe592 · **Ticket**: KAFKA-17749
**Subsystem**: storage/internals (Throttler metrics)
**Category**: Wrong constant / default value

**Root cause**
The Java `Throttler` class used `Throttler.class` as the metrics group, giving it the package-qualified group `org.apache.kafka.storage.internals.utils`. Legacy code expected the group `kafka.utils` (from the original Scala `Throttler`). Metrics published under the wrong group were invisible to monitoring tooling that looked for the old group name, silently losing throttle metrics after the class was migrated from Scala to Java.

**Before/After**
```java
// Before
new KafkaMetricsGroup(Throttler.class);
// → group = "org.apache.kafka.storage.internals.utils", name = "Throttler"

// After
new KafkaMetricsGroup("kafka.utils", "Throttler");
// → group = "kafka.utils", name = "Throttler"
```

**How to spot**
When migrating a class from Scala to a different Java package, verify that any `KafkaMetricsGroup` or `MetricName` construction explicitly specifies the legacy group string rather than deriving it from the new class object; derive-from-class picks up the new package name automatically.

**Test**
Unit tests updated in `ThrottlerTest` to verify the correct metric group.
