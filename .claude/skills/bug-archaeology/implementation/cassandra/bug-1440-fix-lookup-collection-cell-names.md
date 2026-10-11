# Bug 1440: Fix 2i Lookup on Collection Cell Names With Some Clustering Columns

**Commit**: f54cd98d26 · **Ticket**: CASSANDRA-8073
**Subsystem**: src/java/org/apache/cassandra/db/index/composites/CompositesIndexOnCollectionKey.java
**Category**: Off-by-one

---

## Root Cause

When building the index lookup key for a secondary index on a collection column, the code iterated over clustering column components up to `count - 1` without capping to the actual size of the `cellName`. For tables with composite primary keys where only some clustering columns are specified in the query, `cellName.size()` is smaller than `count - 1`, causing an `IndexOutOfBoundsException` when trying to read beyond the available components.

### Before / After

```java
// BEFORE
for (int i = 0; i < count - 1; i++)
    builder.add(cellName.get(i));
```

```java
// AFTER
for (int i = 0; i < Math.min(cellName.size(), count - 1); i++)
    builder.add(cellName.get(i));
```

---

## How to Spot

When iterating over a composite cell name to extract components, always guard against the actual component count rather than a schema-derived count. Any loop bound computed from schema metadata (`clusteringColumns().size()`) that is applied directly to a runtime `CellName` object without a `Math.min` or length check is suspect, because the runtime name may contain fewer components than the schema maximum.

---

## Test

`ContainsRelationTest.testIndexLookupWithClusteringPrefix` — inserts rows into a table with a three-column primary key `(a, b, c)` and a secondary index on a set column `d`, then verifies that `CONTAINS` queries that supply only a prefix of the clustering columns (`a` and `b`) return correct results without throwing an exception.
