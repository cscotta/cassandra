**Commit**: fb2ce76b49 · **Ticket**: KAFKA-18888
**Subsystem**: checkstyle/import-control-metadata.xml
**Category**: Logic error in condition

**Root cause**
This also adds metrics to StandardAuthorizer

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Ken Huang
 <s7133700@gmail.com>, Jhen-Yung Hsu <jhenyunghsu@gmail.com>, TaiJuWu
 <tjwu1217@gmail.com>

**Before/After**
```java
// Before
    public static <T> Plugin<T> wrapInstance(T instance, Metrics metrics, String key, Map<String, String> extraTags) {
        Map<String, String> tags = tags(key, instance);
        tags.putAll(extraTags);
        return wrapInstance(instance, metrics, () -> tags);
    public static <T> Plugin<T> wrapInstance(T instance, Metrics metrics, Supplier<Map<String, String>> tagsSupplier) {
        PluginMetricsImpl pluginMetrics = null;
        if (instance instanceof Monitorable && metrics != null) {
            pluginMetrics = new PluginMetricsImpl(metrics, tagsSupplier.get());
            ((Monitorable) instance).withPluginMetrics(pluginMetrics);
        }
        return new Plugin<>(instance, pluginMetrics);
    }

// After
            <allow pkg="org.apache.kafka.common.metrics" />
            <allow pkg="org.apache.kafka.common.metrics.internals" />
            <allow pkg="org.apache.kafka.common.metrics.stats" />
    <allow pkg="org.apache.kafka.server" />
/**
 * Plugins have the following tags:
 * <ul>
 *   <li><code>config</code> set to the name of the configuration to specifying the plugin</li>
 *   <li><code>class</code> set to the name of the instance class</li>
 * </ul>
 */
    /**
```

**How to spot**
When reviewing checkstyle/import-control-metadata.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18888: add kip-877 support to authorizer (#19050).

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
