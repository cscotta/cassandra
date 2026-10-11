# Bug 0456: Short Read Protection Broken for GROUP BY Queries

**Commit**: 86a9261fd9 · **Ticket**: CASSANDRA-15459
**Subsystem**: `src/java/org/apache/cassandra/db/filter/DataLimits.java`, `src/java/org/apache/cassandra/service/DataResolver.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The `GroupByAwareCounter` inner class declared its own `rowCounted` and `rowCountedInCurrentPartition` fields that shadowed — but did not override — identically-named fields in the parent `CQLCounter`. The `DataResolver` short-read protection (SRP) logic used `counter.rowCounted()` to determine how many rows had been returned for GROUP BY queries, but the method dispatched to the wrong (parent) fields, always returning a stale or zero value. This caused SRP to issue unnecessary re-read requests and, more critically, to miscount the rows available for the current page, leading to wrong results when deletions were involved. A secondary issue was that the GROUP BY counter did not check `enforceLimits` before discarding rows in static-row and regular-row handlers, causing rows to be dropped silently during SRP re-reads.

### Before / After

```java
// BEFORE — GroupByAwareCounter had identically-named fields, causing shadowing
protected int rowCounted;
protected int rowCountedInCurrentPartition;

// DataResolver used the ambiguous method name which resolved to parent fields
return command.limits().isGroupByLimit()
     ? counter.rowCounted()   // returned parent CQLCounter.rowCounted (wrong)
     : counter.counted();
```

```java
// AFTER — fields and methods renamed to rowsCounted / rowsCountedInCurrentPartition
protected int rowsCounted;
protected int rowsCountedInCurrentPartition;

return command.limits().isGroupByLimit()
     ? counter.rowsCounted()  // unambiguous, dispatches to GroupByAwareCounter
     : counter.counted();

// Also, limit enforcement now gated on enforceLimits flag
if (enforceLimits && isDone())
    ...
```

---

## How to Spot

When a subclass declares fields with the same name as the parent, any method that reads the parent-named field will silently use the parent's value rather than the subclass override — Java does not allow field polymorphism. Reviewers should check that overriding counter or accumulator classes in a hierarchy do not re-declare fields with names already present in a superclass; using distinct names or a single abstract accessor in the base class eliminates the ambiguity. Additionally, any code that suppresses rows based on a limit should check a `enforceLimits` guard so that internal re-read passes used by short-read protection are not accidentally subject to the same filtering as client-visible pages.

---

## Test

A distributed two-node test inserts divergent data on each replica (with deletions so that reconciliation can yield fewer rows than the limit) and then asserts that `SELECT ... GROUP BY ... LIMIT N` at consistency ALL returns the same (empty) result as without a limit, verifying that SRP does not corrupt GROUP BY paged results.
