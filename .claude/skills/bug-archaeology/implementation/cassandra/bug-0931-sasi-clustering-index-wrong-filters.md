# Bug 0931: Wrong Filters with SASI Clustering Index

**Commit**: 3fda52f247 · **Ticket**: CASSANDRA-11397
**Subsystem**: SASI secondary index / CQL restrictions
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`PrimaryKeyRestrictionSet` tracked whether clustering restrictions were equality (`eq`), IN (`in`), slice, or contains — but had no state for LIKE restrictions. When a SASI LIKE predicate was applied to a clustering column, the restriction set fell through to the `eq` branch, which caused downstream logic in `clusteringBounds()` and `getIndexExpressions()` to treat the LIKE restriction as if it were an equality, incorrectly applying clustering bounds and skipping the LIKE expression from the index filter. The result was that the SASI index was not consulted and/or the clustering filter was computed incorrectly, returning wrong rows. The fix adds a `like` boolean field to `PrimaryKeyRestrictionSet`, overrides `isLIKE()`, and guards both the clustering-bounds loop and the index-expression loop to break out / include the expression correctly when a LIKE restriction is present.

### Before / After
```java
// BEFORE — LIKE restriction falls into eq branch; bounds loop doesn't skip it
if (keyPosition != def.position() || r.isContains())
    break;

// AFTER — LIKE also breaks out of the bounds loop
if (keyPosition != def.position() || r.isContains() || r.isLIKE())
    break;

// BEFORE — LIKE restriction treated as eligible for slice/skip
if (!isPartitionKey && !restriction.isContains() && position == columnDef.position())

// AFTER
if (!isPartitionKey && !(restriction.isContains() || restriction.isLIKE()) && position == columnDef.position())
```

---

## How to Spot
Whenever a new restriction type is added to CQL (e.g., LIKE, BETWEEN, ANN), check that `PrimaryKeyRestrictionSet` (and any other `AbstractPrimaryKeyRestrictions` implementation) correctly propagates the new `isXxx()` predicate and that every place that iterates or filters restrictions handles the new type. Missing a predicate in the type-dispatch chain causes the new restriction to silently fall into a wrong branch.

---

## Test
`SASIIndexTest` was extended with new test cases covering LIKE queries on clustering columns to verify correct row filtering.
