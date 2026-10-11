# Bug 0722: JVM metric prefix names have trailing dot, producing double-dot paths

**Commit**: a79cc9840d · **Ticket**: CASSANDRA-13103
**Subsystem**: Metrics / CassandraDaemon
**Category**: Wrong constant / default value

---

## Root Cause
The JVM metric set prefixes passed to `CassandraMetricsRegistry.Metrics.register()` each had a trailing dot (e.g., `"jvm.buffers."`, `"jvm.gc."`, `"jvm.memory."`). The metrics library appends its own separator when constructing the full metric name, so the registered names contained a double dot — for example `jvm.buffers..pool.direct.capacity` — making them invisible to external reporters expecting canonical names like `jvm.buffers.pool.direct.capacity`. The fix removes the trailing dots from the three prefix strings.

### Before / After
```java
// BEFORE
CassandraMetricsRegistry.Metrics.register("jvm.buffers.", new BufferPoolMetricSet(...));
CassandraMetricsRegistry.Metrics.register("jvm.gc.",      new GarbageCollectorMetricSet());
CassandraMetricsRegistry.Metrics.register("jvm.memory.",  new MemoryUsageGaugeSet());

// AFTER
CassandraMetricsRegistry.Metrics.register("jvm.buffers", new BufferPoolMetricSet(...));
CassandraMetricsRegistry.Metrics.register("jvm.gc",      new GarbageCollectorMetricSet());
CassandraMetricsRegistry.Metrics.register("jvm.memory",  new MemoryUsageGaugeSet());
```

---

## How to Spot
When registering metric sets that act as a namespace prefix, check whether the registry already appends a separator. If the library joins prefix and child name with a dot, the prefix must not end with a dot. Compare the registered prefix strings against the Dropwizard / Codahale convention, which does not include a trailing separator in the base name.

---

## Test
No test added.
