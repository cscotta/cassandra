# Bug 1588: Fix Duplicate Field Declaration and Expanded KeyspaceMetrics After Merge

**Commit**: 9baae7b807 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/Keyspace.java, src/java/org/apache/cassandra/metrics/KeyspaceMetrics.java
**Category**: State not cleaned up

---

## Root Cause

A botched merge left `Keyspace.java` with two identical `public final KeyspaceMetrics metric;` field declarations, which is a compile error. At the same time, `KeyspaceMetrics` was missing the full set of on-heap/off-heap memtable metric gauges introduced for the `Keyspace`-based branch, with `pendingTasks` incorrectly used instead of the per-CF `pendingFlushes` aggregation. The fix removes the duplicate field and expands `KeyspaceMetrics` with the proper on-heap, off-heap, and live data size gauges.

### Before / After

```java
// BEFORE — duplicate field in Keyspace.java
public final KeyspaceMetrics metric;
public final KeyspaceMetrics metric;  // compile error
```

```java
// AFTER — single declaration
public final KeyspaceMetrics metric;
```

---

## How to Spot

After any merge that touches class field declarations, scan for duplicate `public final` fields with the same name. A simple `javac` build catches this immediately. For metrics classes, verify that every gauge registered in the constructor has a corresponding `removeMetric` call in `release()` and that all referenced sub-metric names actually exist on the referenced `ColumnFamilyStore.metric` object.

---

## Test

No test added.
