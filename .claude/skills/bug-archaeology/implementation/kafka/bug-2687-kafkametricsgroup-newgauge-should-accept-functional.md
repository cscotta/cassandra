**Commit**: e9a8c3c455 · **Ticket**: KAFKA-17153
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import com.yammer.metrics.core.Gauge;
        metricsGroup.newGauge(REMOTE_LOG_MANAGER_TASKS_AVG_IDLE_PERCENT_METRIC, new Gauge<Double>() {
            @Override
            public Double value() {
                return rlmCopyThreadPool.getIdlePercent();
            }
        });
  private def recreateGauge[T](name: String, gauge: Gauge[T]): Gauge[T] = {
    metricsGroup.newGauge(name, gauge)
    public final <T> Gauge<T> newGauge(String name, Gauge<T> metric, Map<String, String> tags) {
        return KafkaYammerMetrics.defaultRegistry().newGauge(metricName(name, tags), metric);
    public final <T> Gauge<T> newGauge(String name, Gauge<T> metric) {

// After
        metricsGroup.newGauge(REMOTE_LOG_MANAGER_TASKS_AVG_IDLE_PERCENT_METRIC, rlmCopyThreadPool::getIdlePercent);
import java.util.function.Supplier
  private def recreateGauge[T](name: String, metric: Supplier[T]): Gauge[T] = {
    metricsGroup.newGauge(name, metric)
import java.util.function.Supplier;
    public <T> Gauge<T> newGauge(String name, Supplier<T> metric, Map<String, String> tags) {
        return newGauge(metricName(name, tags), metric);
    public <T> Gauge<T> newGauge(String name, Supplier<T> metric) {
    public <T> Gauge<T> newGauge(MetricName name, Supplier<T> metric) {
        return KafkaYammerMetrics.defaultRegistry().newGauge(name, new Gauge<T>() {
            @Override
            public T value() {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17153 kafkametricsgroup#newgauge should accept functional interface instead of `com.yammer.metrics.core.gague` (#1.

**Test**
Test modified in RemoteStorageThreadPool.java covering the corrected behaviour.
