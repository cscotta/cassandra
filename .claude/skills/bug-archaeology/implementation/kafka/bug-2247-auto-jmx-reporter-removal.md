**Commit**: 3815339e05 · **Ticket**: KAFKA-14158
**Subsystem**: clients (CommonClientConfigs, ConsumerConfig, ProducerConfig, AdminClientConfig)
**Category**: State not cleaned up

**Root cause**
`auto.include.jmx.reporter` was a deprecated boolean config that silently auto-injected `JmxReporter` into the metrics reporter list even if the user had not listed it. This made the metrics reporter list non-transparent: users could not reliably enumerate active reporters, and the implicit injection bypassed the explicit `metric.reporters` contract. The fix removes the config and makes `JmxReporter` the explicit default value of `metric.reporters`, giving users full control.

**Before/After**
```java
// Before – implicit injection
if (config.getBoolean(AUTO_INCLUDE_JMX_REPORTER_CONFIG)
    && reporters.stream().noneMatch(r -> JmxReporter.class.equals(r.getClass()))) {
    reporters.add(new JmxReporter());
}

// After – JmxReporter is explicit default
.define(METRIC_REPORTER_CLASSES_CONFIG, Type.LIST,
    JmxReporter.class.getName(), ...)
// to disable: set metric.reporters=""
```

**How to spot**
Any "auto-include" boolean that appends to a user-visible list config creates a transparency problem; replace it by making the desired default the actual default value of the list config so `config.describe()` shows the true active set.

**Test**
`CommonClientConfigsTest.testMetricsReporters` updated to use `metric.reporters=""` instead of `auto.include.jmx.reporter=false`.
