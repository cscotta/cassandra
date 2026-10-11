**Commit**: 5736d506f0 · **Ticket**: KAFKA-19684
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
KIP-188 introduced MetricValueProvider, adding Measurable and Gauge as
its sub interfaces. However, this left a legacy

[issue](https://github.com/apache/kafka/pull/3705#discussion_r140830112):
move the value method from Gauge to the super interface,
MetricValueProvider. This PR moves the value method from Gauge to MetricValueProvider and
provides a default implementation in Measurable. This unifies the
methods used by Gauge and Measurable to obtain monitoring values.

**Before/After**
```java
// Before
public interface Gauge<T> extends MetricValueProvider<T> {

    /**
     * Returns the current value associated with this gauge.
     * @param config The configuration for this metric
     * @param now The POSIX time in milliseconds the measurement is being taken
     */
    T value(MetricConfig config, long now);

}
        if (!(valueProvider instanceof Measurable) && !(valueProvider instanceof Gauge))
            throw new IllegalArgumentException("Unsupported metric value provider of class " + valueProvider.getClass());

// After
public interface Gauge<T> extends MetricValueProvider<T> { }
import java.util.Objects;

        this.metricValueProvider = Objects.requireNonNull(valueProvider, "valueProvider must not be null");
     * Take the metric and return the value via {@link MetricValueProvider#value(MetricConfig, long)}.
     *
            return metricValueProvider.value(config, now);
     * Measure this quantity and return the result as a double.
     *
    /**
     * Measure this quantity and return the result as a double.
     *
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19684 move gauge#value to metricvalueprovider (#20543).

**Test**
Test modified in KafkaMetricTest.java covering the corrected behaviour.
