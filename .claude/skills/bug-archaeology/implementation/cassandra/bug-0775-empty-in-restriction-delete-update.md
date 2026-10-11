# Bug 0775: DELETE/UPDATE with Empty IN Restriction Crashes or Produces Wrong Results

**Commit**: 9fc1ffb631 · **Ticket**: CASSANDRA-12829
**Subsystem**: CQL / DML statements
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`ModificationStatement.executeWithCondition()` and `BatchStatement.executeViaCas()` guarded against multi-partition or multi-clustering conditional writes by checking `keys.size() > 1` or `clusterings.size() > 1`. When an `IN (...)` restriction was given with an *empty* list, `buildPartitionKeyNames()` returned an empty list and `createClustering()` returned an empty set, so the size was 0 — passing both guards silently. The subsequent `Iterables.getOnlyElement(...)` call then threw a `NoSuchElementException` because the collection was empty.

For unconditional `DELETE`/`UPDATE` the same empty-`IN` path fell through to `addUpdateForKey` being skipped because `clusterings.isEmpty()` was true — correct in the no-restriction case but wrong in the "restriction present but empty" case.

The fix replaces size-based guards with restriction-type checks (`keyIsInRelation()` / `clusteringKeyRestrictionsHasIN()`) that fire whenever an IN relation is present regardless of how many values it contains, and replaces the `clusterings.isEmpty()` guard in the unconditional path with `!restrictions.hasClusteringColumnsRestriction()`.

### Before / After
```java
// BEFORE: guard passes silently when IN list is empty (size == 0, not > 1)
List<ByteBuffer> keys = buildPartitionKeyNames(options);
checkFalse(keys.size() > 1, "IN on the partition key is not supported...");
SortedSet<Clustering> clusterings = createClustering(options);
checkFalse(clusterings.size() > 1, "IN on the clustering key columns is not supported...");
Clustering clustering = Iterables.getOnlyElement(clusterings); // throws if empty

// unconditional path:
if (clusterings.isEmpty())          // true for no-restriction AND empty-IN
    addUpdateForKey(upd, Clustering.EMPTY, params);

// AFTER: reject whenever an IN relation exists, regardless of list size
checkFalse(restrictions.keyIsInRelation(), "IN on the partition key is not supported...");
checkFalse(restrictions.clusteringKeyRestrictionsHasIN(), "IN on the clustering key columns is not supported...");
Clustering clustering = Iterables.getOnlyElement(createClustering(options));

// unconditional path uses correct predicate:
if (!restrictions.hasClusteringColumnsRestriction())
    addUpdateForKey(upd, Clustering.EMPTY, params);
```

---

## How to Spot
When validating cardinality for IN restrictions, checking the *resolved size* of the value list rather than the *presence* of an IN relation type is fragile: an empty list passes size > 1 checks but then causes `getOnlyElement` to throw. Always validate the *kind* of restriction at parse/prepare time rather than the runtime collection size.

---

## Test
`BatchTest`, `DeleteTest`, `UpdateTest`, and `InsertUpdateIfConditionTest` all extended with empty-IN cases.
