# Bug 1489: Fix Dropping Collection Column When It's the Last Non-PK Column

**Commit**: c44526b296 · **Ticket**: CASSANDRA-7744
**Subsystem**: src/java/org/apache/cassandra/config, src/java/org/apache/cassandra/cql3/statements
**Category**: Logic error in condition

---

## Root Cause

`CFMetaData.rebuildCQL3Metadata()` determined whether a column family was "dense" on every rebuild by calling the static `isDense()` inference heuristic, discarding any previously known value. The heuristic uses the presence of regular column definitions to infer non-density, but when the last regular collection column is dropped there are no more regular definitions left, so the heuristic incorrectly re-classifies the table as dense. This caused the wrong number of clustering key columns to be computed and broke the column family's internal metadata. The fix persists `isDense` as an explicit nullable field that is written to the schema, computed once at `CREATE TABLE` time via CQL, and only inferred by the heuristic when the value is not yet known (null).

### Before / After

```java
// BEFORE  (rebuildCQL3Metadata)
boolean isDense = isDense(comparator, column_metadata.values());
int nbCkCols = isDense
             ? comparator.componentsCount()
             : comparator.componentsCount() - (hasCollection() ? 2 : 1);
```

```java
// AFTER
if (isDense == null)
    setDense(isDense(comparator, column_metadata.values()));
int nbCkCols = isDense
             ? comparator.componentsCount()
             : comparator.componentsCount() - (hasCollection() ? 2 : 1);
```

---

## How to Spot

Any heuristic that infers schema/structural metadata from the current column set is fragile after column removals. Derived structural properties that can be computed definitively at creation time should be stored explicitly and only fall back to inference for legacy rows.

---

## Test

No test added.
