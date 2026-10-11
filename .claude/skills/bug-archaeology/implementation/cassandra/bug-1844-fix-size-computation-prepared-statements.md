# Bug 1844: Fix Size Computation of Prepared Statements

**Commit**: 0ffa5c20af · **Ticket**: CASSANDRA-6369
**Subsystem**: src/java/org/apache/cassandra/cql3 (CQL query processor / prepared statement cache)
**Category**: Wrong constant / default value

---

## Root Cause

The prepared-statement cache used `meter.measureDeep(key)` for every cached statement, which walks the entire object graph including the shared `cfDef` (column family definition) object. Because `cfDef` is shared across all statements for a given table, its size was counted redundantly for every cached statement, causing the cache's memory bookkeeping to wildly overestimate occupancy. The fix introduces a `MeasurableForPreparedCache` interface so each statement type can subtract the shared `cfDef` size and report only the statement-exclusive memory.

### Before / After

```java
// BEFORE (QueryProcessor.java)
private static long measure(Object key)
{
    return MemoryMeter.isInitialized() ? meter.measureDeep(key) : 1;
}
```

```java
// AFTER (QueryProcessor.java)
private static long measure(Object key)
{
    if (!MemoryMeter.isInitialized())
        return 1;

    return key instanceof MeasurableForPreparedCache
         ? ((MeasurableForPreparedCache)key).measureForPreparedCache(meter)
         : meter.measureDeep(key);
}

// Example in SelectStatement / UpdateStatement / DeleteStatement:
public long measureForPreparedCache(MemoryMeter meter)
{
    return meter.measureDeep(this) - meter.measureDeep(cfDef);
}
```

---

## How to Spot

When a cache uses deep-heap measurement for eviction, check whether any large sub-objects are shared across entries; double-counting shared objects inflates the apparent size and causes premature or inaccurate eviction. Introduce a custom measurement interface so shared sub-graphs are excluded.

---

## Test

No test added.
