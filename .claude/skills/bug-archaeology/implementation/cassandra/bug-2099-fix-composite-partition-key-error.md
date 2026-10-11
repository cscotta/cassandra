# Bug 2099: CQL3 Composite Partition Key Range Bound Built With Wrong End-of-Component Marker

**Commit**: 03b4c2794c · **Ticket**: CASSANDRA-5240
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectStatement.buildBound` appended an end-of-component (EOC) byte when building the partition key bound for a fully-specified equality (`EQ`) predicate. For clustering columns this is harmless, but the same method is used to construct partition key bounds for secondary-index queries. With the random partitioner, `buildAsEndOfRange()` is not guaranteed to sort after `builder.build()`, so queries that specified an equality on the partition key alongside a secondary index could return incorrect results. The fix skips the EOC when the builder is already full (i.e., `remainingCount() == 0`), and also stops propagating the `isReversed` flag into `getKeyBound` since reversal is a column-ordering concern, not a partition-key-bound concern.

### Before / After

```java
// BEFORE
return (bound == Bound.END) ? builder.buildAsEndOfRange() : builder.build();

// getKeyBound passed isReversed:
return buildBound(b, cfDef.keys.values(), keyRestrictions, isReversed, ...);
```

```java
// AFTER
return (bound == Bound.END && builder.remainingCount() > 0) ? builder.buildAsEndOfRange() : builder.build();

// getKeyBound always passes false for reversed:
return buildBound(b, cfDef.keys.values(), keyRestrictions, false, ...);
```

---

## How to Spot

Any method that constructs bounds for both partition keys and clustering columns deserves scrutiny: partition key ordering is controlled by the partitioner (and EOC semantics may not apply), while clustering column ordering is schema-defined. Passing `isReversed` into a shared bound-building utility is a warning sign that the two cases have not been properly separated.

---

## Test

No test added.
