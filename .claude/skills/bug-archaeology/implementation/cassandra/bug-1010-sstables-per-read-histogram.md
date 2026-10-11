# Bug 1010: SSTablesPerReadHistogram not updated on row-cache hit

**Commit**: acdbba7957 · **Ticket**: CASSANDRA-10585
**Subsystem**: Read path / metrics
**Category**: Logic error in condition

---

## Root Cause
`ColumnFamilyStore.getColumnFamily()` updates the `sstablesPerReadHistogram` metric only when SSTables are actually read.  On a row-cache hit the method returned early via `filterColumnFamily(cachedCf, filter)` without calling `metric.updateSSTableIterated(0)`, so the histogram was never incremented for cache-hit reads.  This caused the histogram to report skewed statistics: the sample count was lower than the actual read count, and the mean/percentile values were inflated because the 0-SSTable observations were missing.

### Before / After
```java
// BEFORE
if (cacheResult == RowCache.VALID)
{
    metric.rowCacheHit.inc();
    Tracing.trace("Row cache hit");
    return filterColumnFamily(cachedCf, filter);  // exits without updating histogram
}

// AFTER
if (cacheResult == RowCache.VALID)
{
    metric.rowCacheHit.inc();
    Tracing.trace("Row cache hit");
    ColumnFamily result = filterColumnFamily(cachedCf, filter);
    metric.updateSSTableIterated(0);  // record 0 SSTables read
    return result;
}
```

---

## How to Spot
Whenever a read path has multiple early-exit branches (cache hit, short-circuit, empty result), audit each branch to ensure all relevant metrics are updated before returning.  Histogram metrics are especially easy to miss because the default "0 count" path is invisible — the bug only surfaces as a statistical anomaly in the histogram rather than a crash.

---

## Test
`RowCacheTest.testSSTablesPerReadHistogramWhenRowCache()` added — inserts 100 rows, forces a flush, then performs alternating first-read (SSTable) and second-read (cache-hit) lookups and asserts that the histogram count increases on every read and that the 49th-percentile and mean values reflect the mix of 0-SSTable and 1-SSTable observations.
