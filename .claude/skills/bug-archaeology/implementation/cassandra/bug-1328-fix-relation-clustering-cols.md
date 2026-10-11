# Bug 1328: Token Function Combined With Multi-Column Clustering Relation Causes ClassCastException

**Commit**: ee160a971a · **Ticket**: CASSANDRA-8797
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Missing null / bounds check

---

## Root Cause

The `WHERE` clause validation loop in `SelectStatement` iterated over every relation and unconditionally cast each one to `SingleColumnRelation` in order to check the `onToken` flag. When a query combined a `token()` restriction on the partition key with a multi-column clustering relation (e.g. `(c, d) > (0, 0)`), the multi-column relation reached the cast and threw a `ClassCastException`. The fix introduces a base-class `isOnToken()` method that returns `false` by default, so non-token relations are skipped before any cast is attempted.

### Before / After

```java
// BEFORE
SingleColumnRelation singleColumnRelation = (SingleColumnRelation) relation;  // throws on multi-column
if (singleColumnRelation.onToken && !cfDef.get(...).equals(iter.next()))
    throw new InvalidRequestException(...);
```

```java
// AFTER
if (!relation.isOnToken())
    continue;
assert !relation.isMultiColumn() : "Unexpectedly got multi-column token relation";
SingleColumnRelation singleColumnRelation = (SingleColumnRelation) relation;
if (!cfDef.get(...).equals(iter.next()))
    throw new InvalidRequestException(...);
```

---

## How to Spot

Whenever a loop casts an abstract/base-class collection element to a concrete subtype without first checking what subtype it is, any future addition of another subtype will silently break the path. Reviewers should look for unconditional downcasts inside loops that iterate over polymorphic collections — the guard should always use the type's own predicate method (`isMultiColumn()`, `isOnToken()`, etc.) rather than relying on assumptions about what kinds of objects can appear.

---

## Test

`SelectWithTokenFunctionTest.testTokenFunctionWithCompoundPartitionAndClusteringCols` — exercises token restrictions combined with both single-column and multi-column clustering restrictions to confirm no exception is thrown.
