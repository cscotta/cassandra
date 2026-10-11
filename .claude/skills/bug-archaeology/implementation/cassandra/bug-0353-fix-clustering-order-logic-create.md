# Bug 0353: Wrong Clustering Order in CREATE MATERIALIZED VIEW When Base Table Has Its Own Clustering Order

**Commit**: af17f136e5 · **Ticket**: CASSANDRA-16898
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/schema (CreateViewStatement)
**Category**: Logic error in condition

---

## Root Cause

`CreateViewStatement.getType()` used `clusteringOrder.getOrDefault(name, true)` to decide whether a column's sort direction should be reversed.  For columns not explicitly listed in the MV's `WITH CLUSTERING ORDER BY` clause, this defaulted to ascending (`true`), overriding the direction already encoded in the base table's `ReversedType`.  If the base column was stored as `ReversedType` (i.e. `DESC` in the base table), but the MV declaration omitted that column from its own `CLUSTERING ORDER BY`, the type was incorrectly unwrapped, making the MV store that column in the opposite order to what the base table intended.

### Before / After

```java
// BEFORE
boolean reverse = !clusteringOrder.getOrDefault(name, true);

if (type.isReversed() && !reverse)
    return ((ReversedType) type).baseType;
else if (!type.isReversed() && reverse)
    return ReversedType.getInstance(type);
else
    return type;
```

```java
// AFTER
if (clusteringOrder.containsKey(name))
{
    boolean reverse = !clusteringOrder.get(name);

    if (type.isReversed() && !reverse)
        return ((ReversedType) type).baseType;

    if (!type.isReversed() && reverse)
        return ReversedType.getInstance(type);
}
return type;
```

---

## How to Spot

Any time a `Map.getOrDefault` is used to control a type transformation, verify that the default does not silently change the semantics for keys that were intentionally absent.  For `CLUSTERING ORDER BY`, absent means "inherit from the base table", not "use ASC"; treating them differently corrupts the stored column type.

---

## Test

Creates a base table with `CLUSTERING ORDER BY (c1 DESC, c2 ASC, c3 DESC)`, creates MVs with partial and full `WITH CLUSTERING ORDER BY` clauses, inserts rows, then asserts the query results from the MVs arrive in the correct order.
