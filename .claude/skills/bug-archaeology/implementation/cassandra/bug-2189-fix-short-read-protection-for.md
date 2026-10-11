# Bug 2189: Short read protection counted raw columns instead of logical CQL3 cells

**Commit**: 353309f013 · **Ticket**: CASSANDRA-4882
**Subsystem**: src/java/org/apache/cassandra/db/filter/SliceQueryFilter.java, src/java/org/apache/cassandra/service/RowRepairResolver.java, src/java/org/apache/cassandra/db/SliceFromReadCommand.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Short read protection compares how many live columns each replica returned against the requested count to decide whether a retry with a larger page is needed. For CQL3 tables, a single logical row can be stored as multiple internal columns (one per CQL cell), so `ColumnFamily.getLiveColumnCount()` over-counted: it returned the raw internal column count rather than the CQL-level "group" count. `RowRepairResolver` used this inflated number for `maxLiveColumns`, causing the coordinator to incorrectly conclude that a node had returned a full page when it had not, suppressing the retry. The fix introduces `SliceQueryFilter.getLiveCount(ColumnFamily)` which applies the same `ColumnCounter` (including `GroupByPrefix` logic for composite types) used during normal collection, and threads the query filter through `RowRepairResolver` so it is available at repair time.

### Before / After

```java
// BEFORE — raw internal column count, wrong for CQL3
int liveColumns = cf == null ? 0 : cf.getLiveColumnCount();
if (liveColumns > maxLiveColumns)
    maxLiveColumns = liveColumns;
```

```java
// AFTER — CQL3-aware logical cell count
int liveCount = cf == null ? 0 : (filter == null ? cf.getLiveColumnCount() : filter.getLiveCount(cf));
if (liveCount > maxLiveCount)
    maxLiveCount = liveCount;
```

---

## How to Spot

Whenever pagination or short-read-protection logic compares a "live count" against a user-requested count, verify that the counting method is consistent with how the query layer defines a "row" or "cell". For CQL3 on composite tables, raw `getLiveColumnCount()` is never the right metric — always use the filter's own `getLiveCount` which accounts for grouping.

---

## Test

No test added.
