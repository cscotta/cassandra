**Commit**: 676e0f2ad6 · **Ticket**: KAFKA-19139
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
There will be an update to the PluginMetrics#metricName method: the type
of the tags parameter will be changed
from Map to LinkedHashMap. This change is necessary because the order of metric tags is important
1. If the tag order is inconsistent, identical metrics may be treated as
distinct ones by the metrics backend
2. KAFKA-18390 is updating metric naming to use LinkedHashMap.

**Before/After**
```java
// Before
            MetricName metricName = metrics.metricName(METRIC_NAME, METRIC_DESCRIPTION, Map.of());
import java.util.Map;
    MetricName metricName(String name, String description, Map<String, String> tags);
    public MetricName metricName(String name, String description, Map<String, String> tags) {
    private static final Map<String, String> TAGS = Collections.singletonMap("k", "v");
        Properties baseProps = new Properties() {{
                setProperty(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9999");
                setProperty(
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                setProperty(
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// After
            MetricName metricName = metrics.metricName(METRIC_NAME, METRIC_DESCRIPTION, new LinkedHashMap<>());
import java.util.LinkedHashMap;
    MetricName metricName(String name, String description, LinkedHashMap<String, String> tags);
    public MetricName metricName(String name, String description, LinkedHashMap<String, String> tags) {
    private static final LinkedHashMap<String, String> TAGS = new LinkedHashMap<>();
    static {
        TAGS.put("t1", "v1");
    }

        Properties baseProps = baseProperties();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19139 plugin#wrapinstance should use linkedhashmap instead of map (#19519).

**Test**
Test modified in CustomQuotaCallbackTest.java covering the corrected behaviour.
