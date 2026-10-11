# Bug 0748: UPDATE with empty IN restriction executes unnecessarily

**Commit**: fb606dd41c · **Ticket**: CASSANDRA-13152
**Subsystem**: CQL / Query execution
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`ModificationStatement.executeWithoutCondition()` (and the related execute path) called `createClustering(options)` and then unconditionally built `UpdateParameters` and applied mutations, even when the resulting clustering set was empty — which happens when an `IN (...)` restriction on a clustering column is bound to an empty list. In that case, no rows should be touched, but the code proceeded to create parameters and call per-key execute logic, potentially triggering unnecessary reads or writes. The fix adds an early return when clustering restrictions are present but the resolved set is empty.

### Before / After
```java
// BEFORE — proceeds to build UpdateParameters even with empty clusterings
NavigableSet<Clustering> clusterings = createClustering(options);
UpdateParameters params = makeUpdateParameters(keys, clusterings, options, local, now);
for (ByteBuffer key : keys)
    execute(key, ...);

// AFTER — short-circuit when IN restriction resolves to empty
NavigableSet<Clustering> clusterings = createClustering(options);
if (restrictions.hasClusteringColumnsRestriction() && clusterings.isEmpty())
    return;
UpdateParameters params = makeUpdateParameters(keys, clusterings, options, local, now);
```

---

## How to Spot
Any statement execution path that iterates over a resolved restriction set should guard against the empty case before performing I/O. `IN` restrictions with bound lists are the canonical source of empty sets at runtime; every `ModificationStatement` subtype that builds clusterings before writing should check `clusterings.isEmpty()` before proceeding.

---

## Test
Tests were added to `UpdateTest` and `DeleteTest` verifying that `UPDATE ... WHERE ck IN ()` and `DELETE ... WHERE ck IN ()` with empty bound lists produce no result and no error.
