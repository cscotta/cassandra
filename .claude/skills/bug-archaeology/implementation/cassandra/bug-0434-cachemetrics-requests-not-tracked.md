# Bug 0434: CacheMetrics.requests Computed Instead of Tracked, Invisible to Reporters

**Commit**: 32874dd04a · **Ticket**: CASSANDRA-16228
**Subsystem**: `src/java/org/apache/cassandra/metrics/CacheMetrics.java`, `cache/InstrumentingCache.java`
**Category**: Wrong constant / default value

---

## Root Cause

`CacheMetrics.requests` was declared as `Metered` (an interface) and implemented as a synthetic aggregate object that summed `hits` and `misses` at read-time. This synthetic meter was not a real Dropwizard `Meter` and was registered under a name the metric reporter framework did not know how to serialize or export. Additionally, `InstrumentingCache.get()` never called `requests.mark()`, so the counter was purely a derived view with no moving-average state of its own, meaning rate estimates were also incorrect. The fix replaces it with a plain `Meter` and marks it explicitly on every `get()` call.

### Before / After

```java
// BEFORE – synthetic Metered wrapping hits+misses; not a real Meter
public final Metered requests;
requests = Metrics.register(factory.createMetricName("Requests"), sumMeters(hits, misses));
// InstrumentingCache.get() – no requests.mark() call
```

```java
// AFTER – real Meter, marked explicitly on every cache access
public final Meter requests;
requests = Metrics.meter(factory.createMetricName("Requests"));
// InstrumentingCache.get():
metrics.requests.mark();
```

---

## How to Spot

Whenever a metric is implemented as a hand-rolled interface implementation rather than a concrete `Meter`/`Counter`/`Gauge` registered through the metrics factory, it will be invisible to reporters that use type-based serialization (JMX, Prometheus exporters, etc.). Review metrics classes for fields typed as interfaces (`Metered`, `Counting`) instead of concrete types, and check that every code path that increments a named metric calls `.mark()` or `.inc()` rather than relying on derived computation.

---

## Test

No test added for the metric visibility fix itself; the `reset()` method's comment acknowledges the limitation.
