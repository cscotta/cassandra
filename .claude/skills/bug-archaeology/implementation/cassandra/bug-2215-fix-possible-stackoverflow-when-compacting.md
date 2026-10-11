# Bug 2215: StackOverflow When Computing Overlapping SSTables Across Thousands of Files

**Commit**: e400d0eca2 · **Ticket**: CASSANDRA-4765
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Logic error in condition

---

## Root Cause

`ColumnFamilyStore.getOverlappingSSTables()` built up its result set by repeatedly calling `Sets.union()` on the growing result with each new overlap set. `Sets.union()` in Guava returns a lazy view that chains the underlying sets together; with thousands of SSTables, this produced a deeply nested chain of lazy union views. When the final set was iterated (e.g., for `Sets.difference`), resolving the chain caused a stack overflow due to the recursive `contains()` and iterator delegation. The fix calls `.immutableCopy()` after each union to materialize the result into a flat `ImmutableSet`, preventing unbounded view nesting.

### Before / After

```java
// BEFORE
results = results == null ? overlaps : Sets.union(results, overlaps);
```

```java
// AFTER
results = results == null ? overlaps : Sets.union(results, overlaps).immutableCopy();
```

---

## How to Spot

Guava's `Sets.union` and `Sets.difference` return lazy views, not materialized collections. When they are accumulated in a loop (one union per iteration), the resulting view structure grows linearly in depth with the number of iterations. Any code that iterates or calls `contains()` on such a structure will pay O(n) stack depth. Call `.immutableCopy()` or `ImmutableSet.copyOf()` inside the loop to keep the structure flat.

---

## Test

No test added.
