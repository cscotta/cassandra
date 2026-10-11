# Bug 0843: SASI clustering index indexes static row, corrupting results

**Commit**: 5c6cdc60a7 · **Ticket**: CASSANDRA-12378
**Subsystem**: Secondary Indexes / SASI
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`ColumnIndex.getValueOf` is the method that extracts the cell value to be indexed from a given row. For a `CLUSTERING`-kind column it reads `row.clustering().get(column.position())`. When the row being processed is the static row, `row.clustering()` is the empty sentinel `Clustering.EMPTY`, so `get(column.position())` returns `null` or an out-of-range value. The static row was being fed to the clustering column indexer with no guard, producing `NullPointerException`s or silently writing corrupt values into the SASI on-disk index.

### Before / After
```java
// BEFORE
case CLUSTERING:
    return row.clustering().get(column.position());

// AFTER
case CLUSTERING:
    // skip indexing of static clustering when regular column is indexed
    if (row.isStatic())
        return null;

    return row.clustering().get(column.position());
```

---

## How to Spot
Any code path in a secondary index that handles both static and non-static rows must branch on `row.isStatic()` before accessing clustering components. Because the static row has an empty clustering, any attempt to read a clustering position from it will silently return `null` or throw; the real symptom is a missing-result or wrong-result query rather than an obvious exception.

---

## Test
`SASIIndexTest` was extended with a test that creates a table with static columns and a clustering column index and verifies correct query results.
