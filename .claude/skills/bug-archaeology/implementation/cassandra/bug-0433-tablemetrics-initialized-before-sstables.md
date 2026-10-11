# Bug 0433: TableMetrics Initialized Before SSTable Size Tracking, Causing Race

**Commit**: 548ef43856 · **Ticket**: CASSANDRA-16228
**Subsystem**: `src/java/org/apache/cassandra/db/ColumnFamilyStore.java`, `db/lifecycle/Tracker.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

`ColumnFamilyStore` was constructing and registering `TableMetrics` before the initial SSTable set was fully loaded and its disk-space size accounting was applied. Because `ColumnFamilyStore` passes `this` into `TableMetrics` during registration (and metric registries notify listeners synchronously), an external listener could observe a `Gauge` that reads from an SSTable collection that was still being populated. This created a window where `updateSizeTracking` would race against any listener that interrogated the gauge immediately on registration.

### Before / After

```java
// BEFORE – metric registered before sstables loaded, size tracking after
metric = new TableMetrics(this);               // listeners fire here, sstables not yet sized
// ...
data.addInitialSSTables(sstables);             // adds AND sizes in one call
```

```java
// AFTER – sstables added to live set first, metrics registered, then size updated
data.addInitialSSTablesWithoutUpdatingSize(sstables);  // live set populated
// ...
metric = new TableMetrics(this);               // listeners fire; live set is complete
// ...
data.updateInitialSSTableSize(sstables);       // size accounting applied after listeners done
```

---

## How to Spot

When an object registers itself with a notification bus (here, Dropwizard `MetricRegistryListener`) inside a constructor or during a multi-step initialization sequence, look for any gauges or counters that read back into the not-yet-fully-initialized object. If another thread or a synchronous callback can read the metric value before initialization is complete, the construction order must be adjusted so all state feeding the metric is finalized before registration. A useful heuristic: anything calling `Metrics.register(...)` should come last in an initialization sequence, not in the middle.

---

## Test

`testStartupRaceConditionOnMetricListeners` registers a `MetricRegistryListener` before creating a keyspace/CFS and asserts that all gauge/counter callbacks execute without throwing exceptions, verifying the CFS is in a fully initialized state when listeners observe it.
