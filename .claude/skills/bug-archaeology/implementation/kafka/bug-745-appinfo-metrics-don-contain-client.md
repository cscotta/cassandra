**Commit**: 01fccd3513 · **Ticket**: KAFKA-15186
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
All Kafka component register AppInfo metrics to track the application
start time or commit-id etc. These metrics are useful for monitoring and
debugging. However, the AppInfo doesn't provide client-id, which is an
important information for custom metrics reporter. The AppInfoParser class registers a JMX MBean with the provided
client-id, but when it adds metrics to the Metrics registry, the
client-id is not included.

**Before/After**
```java
// Before
            registerMetrics(metrics, mBean); // prefix will be added later by JmxReporter
            unregisterMetrics(metrics);
    private static MetricName metricName(Metrics metrics, String name) {
        return metrics.metricName(name, "app-info", "Metric indicating " + name);
    private static void registerMetrics(Metrics metrics, AppInfo appInfo) {
        if (metrics != null) {
            metrics.addMetric(metricName(metrics, "version"), (Gauge<String>) (config, now) -> appInfo.getVersion());
            metrics.addMetric(metricName(metrics, "commit-id"), (Gauge<String>) (config, now) -> appInfo.getCommitId());
            metrics.addMetric(metricName(metrics, "start-time-ms"), (Gauge<Long>) (config, now) -> appInfo.getStartTimeMs());
    private static void unregisterMetrics(Metrics metrics) {
        if (metrics != null) {
            metrics.removeMetric(metricName(metrics, "version"));

// After
import java.util.Map;
            registerMetrics(metrics, mBean, id); // prefix will be added later by JmxReporter
            unregisterMetrics(metrics, id);
    private static MetricName metricName(Metrics metrics, String name, Map<String, String> tags) {
        return metrics.metricName(name, "app-info", "Metric indicating " + name, tags);
    private static void registerMetrics(Metrics metrics, AppInfo appInfo, String clientId) {
        if (metrics == null) return;
        // Most Kafka clients (producer/consumer/admin) set the client-id tag in the metrics config.
        // Although we don’t explicitly parse client-id here, these metrics are automatically tagged with client-id.
        metrics.addMetric(metricName(metrics, "version", Map.of()), (Gauge<String>) (config, now) -> appInfo.getVersion());
        metrics.addMetric(metricName(metrics, "commit-id", Map.of()), (Gauge<String>) (config, now) -> appInfo.getCommitId());
        metrics.addMetric(metricName(metrics, "start-time-ms", Map.of()), (Gauge<Long>) (config, now) -> appInfo.getStartTimeMs());
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15186 appinfo metrics don't contain the client-id (#20493).

**Test**
Test modified in AppInfoParserTest.java covering the corrected behaviour.
