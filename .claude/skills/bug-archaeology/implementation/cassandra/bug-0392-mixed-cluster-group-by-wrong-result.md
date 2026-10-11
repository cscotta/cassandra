# Bug 0392: Mixed Cluster GROUP BY Returns ArrayIndexOutOfBoundsException

**Commit**: d9e5dd2ff9 · **Ticket**: CASSANDRA-16582
**Subsystem**: src/java/org/apache/cassandra/db/filter/DataLimits.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The `DataLimits.Kind` enum in 4.0 removed two enum constants that existed in 3.x (`THRIFT_LIMIT` and `SUPER_COLUMN_COUNTING_LIMIT`), shifting the ordinal values of `CQL_GROUP_BY_LIMIT` and `CQL_GROUP_BY_PAGING_LIMIT` downward by two positions. When a 4.0 coordinator serialized a `GROUP BY` query limit and sent it to a 3.x node, the 3.x node deserialized the ordinal as a different (or out-of-range) `Kind`, causing an `ArrayIndexOutOfBoundsException`. The fix re-inserts the two deprecated constants as placeholders at their original ordinal positions, restoring wire-format compatibility with 3.x nodes.

### Before / After

```java
// BEFORE
public enum Kind { CQL_LIMIT, CQL_PAGING_LIMIT, CQL_GROUP_BY_LIMIT, CQL_GROUP_BY_PAGING_LIMIT }
```

```java
// AFTER
public enum Kind
{
    CQL_LIMIT,
    CQL_PAGING_LIMIT,
    @Deprecated THRIFT_LIMIT,            // ordinal 2 – kept for 3.x wire compat
    @Deprecated SUPER_COLUMN_COUNTING_LIMIT, // ordinal 3 – kept for 3.x wire compat
    CQL_GROUP_BY_LIMIT,
    CQL_GROUP_BY_PAGING_LIMIT,
}
```

---

## How to Spot

Whenever an enum is serialized by ordinal across a protocol boundary, removing constants from the middle of the enum list silently re-numbers all subsequent constants. During upgrade reviews, check every `enum` that is serialized with `ordinal()` or via `readEnum`/`writeEnum` helpers: any constant removal or insertion that is not at the very end breaks backward compatibility and must be replaced with a deprecated tombstone placeholder instead.

---

## Test

Mixed-version upgrade test (`GroupByTest`) inserts rows on a 3.x cluster, upgrades one node to 4.0, then executes `GROUP BY a,b` from both the 3.x and 4.0 coordinators, asserting the correct aggregated row counts are returned.
